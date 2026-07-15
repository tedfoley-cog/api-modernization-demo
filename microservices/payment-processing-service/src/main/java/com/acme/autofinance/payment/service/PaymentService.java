package com.acme.autofinance.payment.service;

import com.acme.autofinance.events.LateFeesAssessed;
import com.acme.autofinance.events.PaymentAllocated;
import com.acme.autofinance.events.PaymentProcessed;
import com.acme.autofinance.events.PaymentReceived;
import com.acme.autofinance.messaging.DomainEventPublisher;
import com.acme.autofinance.payment.domain.AccountProjection;
import com.acme.autofinance.payment.domain.LoanProjection;
import com.acme.autofinance.payment.domain.Payment;
import com.acme.autofinance.payment.domain.PaymentMethod;
import com.acme.autofinance.payment.domain.PaymentStatus;
import com.acme.autofinance.payment.repository.AccountProjectionRepository;
import com.acme.autofinance.payment.repository.LoanProjectionRepository;
import com.acme.autofinance.payment.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Payment processing service for the payment-processing bounded context. Owns the
 * payment aggregate and allocation/batch logic. All loan and account facts are
 * read from event-sourced local projections; the service never touches another
 * domain's data or beans. Business occurrences are broadcast as domain events via
 * {@link DomainEventPublisher} for other contexts to react to asynchronously.
 */
@Service
public class PaymentService {

    // Hardcoded late fee schedule — preserved from the legacy monolith.
    private static final BigDecimal LATE_FEE_FLAT = new BigDecimal("25.00");
    private static final BigDecimal LATE_FEE_PCT = new BigDecimal("5.00");
    private static final BigDecimal MAX_LATE_FEE = new BigDecimal("50.00");

    private final PaymentRepository paymentRepository;
    private final LoanProjectionRepository loanProjectionRepository;
    private final AccountProjectionRepository accountProjectionRepository;
    private final DomainEventPublisher eventPublisher;

    public PaymentService(PaymentRepository paymentRepository,
                          LoanProjectionRepository loanProjectionRepository,
                          AccountProjectionRepository accountProjectionRepository,
                          DomainEventPublisher eventPublisher) {
        this.paymentRepository = paymentRepository;
        this.loanProjectionRepository = loanProjectionRepository;
        this.accountProjectionRepository = accountProjectionRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Payment submitPayment(Payment payment) {
        payment.setConfirmationNumber("PMT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setPaymentDate(new Date());

        // Validate the loan exists using the local projection — no cross-domain call.
        LoanProjection loan = loanProjectionRepository.findById(payment.getLoanId())
                .orElseThrow(() -> new IllegalArgumentException("Loan not found: " + payment.getLoanId()));

        allocatePayment(payment, loan);

        Payment saved = paymentRepository.save(payment);

        eventPublisher.publish(new PaymentReceived(
                saved.getId(),
                saved.getLoanId(),
                saved.getPaymentAmount(),
                saved.getPaymentMethod() != null ? saved.getPaymentMethod().name() : null,
                saved.getConfirmationNumber()));

        eventPublisher.publish(new PaymentAllocated(
                saved.getId(),
                saved.getLoanId(),
                saved.getPrincipalAmount(),
                saved.getInterestAmount(),
                saved.getFeeAmount()));

        if (payment.getPaymentMethod() == PaymentMethod.ACH) {
            processAchPayment(saved);
        }

        return saved;
    }

    private void allocatePayment(Payment payment, LoanProjection loan) {
        BigDecimal totalAmount = payment.getPaymentAmount();
        BigDecimal monthlyRate = BigDecimal.ZERO;
        if (loan.getInterestRate() != null) {
            monthlyRate = loan.getInterestRate()
                    .divide(new BigDecimal("1200"), 10, RoundingMode.HALF_UP);
        }

        Optional<AccountProjection> accountOpt = accountProjectionRepository.findByLoanId(loan.getLoanId());
        BigDecimal currentBalance = accountOpt.map(AccountProjection::getCurrentBalance)
                .orElse(loan.getApprovedAmount());

        BigDecimal interestPortion = BigDecimal.ZERO;
        if (currentBalance != null) {
            interestPortion = currentBalance.multiply(monthlyRate).setScale(2, RoundingMode.HALF_UP);
        }

        // Allocation order preserved from legacy: fees, then interest, then principal.
        BigDecimal feesPortion = BigDecimal.ZERO;
        BigDecimal remaining = totalAmount;

        List<Payment> pendingFees = paymentRepository.findByLoanIdAndStatus(
                loan.getLoanId(), PaymentStatus.PENDING);
        for (Payment fee : pendingFees) {
            if (fee.getLateFee() != null && fee.getLateFee().compareTo(BigDecimal.ZERO) > 0) {
                feesPortion = feesPortion.add(fee.getLateFee());
            }
        }

        if (feesPortion.compareTo(remaining) > 0) {
            feesPortion = remaining;
        }
        remaining = remaining.subtract(feesPortion);

        if (interestPortion.compareTo(remaining) > 0) {
            interestPortion = remaining;
        }
        remaining = remaining.subtract(interestPortion);

        BigDecimal principalPortion = remaining;

        payment.setFeeAmount(feesPortion);
        payment.setInterestAmount(interestPortion);
        payment.setPrincipalAmount(principalPortion);
    }

    @Transactional
    public void processAchPayment(Payment payment) {
        payment.setStatus(PaymentStatus.PROCESSING);
        paymentRepository.save(payment);

        if (payment.getAchRoutingNumber() == null || payment.getAchRoutingNumber().length() != 9) {
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            return;
        }

        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setProcessedDate(new Date());
        Payment completed = paymentRepository.save(payment);

        BigDecimal newBalance = applyToLocalBalance(completed);

        eventPublisher.publish(new PaymentProcessed(
                completed.getId(),
                completed.getLoanId(),
                LocalDate.now(),
                completed.getStatus().name(),
                completed.getPrincipalAmount(),
                newBalance));
    }

    /**
     * Optimistically updates the local account-balance projection so subsequent
     * allocations reflect this payment. The authoritative balance is owned by the
     * account-servicing context, which reacts to {@code PaymentProcessed}.
     */
    private BigDecimal applyToLocalBalance(Payment payment) {
        Optional<AccountProjection> accountOpt = accountProjectionRepository.findByLoanId(payment.getLoanId());
        if (!accountOpt.isPresent()) {
            return null;
        }
        AccountProjection account = accountOpt.get();
        if (account.getCurrentBalance() == null || payment.getPrincipalAmount() == null) {
            return account.getCurrentBalance();
        }
        BigDecimal newBalance = account.getCurrentBalance().subtract(payment.getPrincipalAmount());
        if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
            newBalance = BigDecimal.ZERO;
        }
        account.setCurrentBalance(newBalance);
        account.setLastPaymentDate(new Date());
        account.setDaysPastDue(0);
        accountProjectionRepository.save(account);
        return newBalance;
    }

    /**
     * Assesses a late fee for a past-due loan using local projection facts and
     * publishes {@link LateFeesAssessed}. Returns the created fee payment, or
     * {@code null} when no account projection exists yet.
     */
    @Transactional
    public Payment assessLateFee(Long loanId, int daysPastDue) {
        Optional<AccountProjection> accountOpt = accountProjectionRepository.findByLoanId(loanId);
        if (!accountOpt.isPresent()) {
            return null;
        }

        AccountProjection account = accountOpt.get();

        BigDecimal lateFee;
        if (daysPastDue <= 30) {
            lateFee = LATE_FEE_FLAT;
        } else {
            lateFee = account.getCurrentBalance()
                    .multiply(LATE_FEE_PCT)
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            if (lateFee.compareTo(MAX_LATE_FEE) > 0) {
                lateFee = MAX_LATE_FEE;
            }
        }

        Payment feePayment = new Payment();
        feePayment.setLoanId(loanId);
        feePayment.setPaymentAmount(BigDecimal.ZERO);
        feePayment.setLateFee(lateFee);
        feePayment.setStatus(PaymentStatus.PENDING);
        feePayment.setPaymentDate(new Date());
        feePayment.setConfirmationNumber("FEE-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        Payment saved = paymentRepository.save(feePayment);

        eventPublisher.publish(new LateFeesAssessed(loanId, lateFee, daysPastDue, LocalDate.now()));

        return saved;
    }

    public List<Payment> getPaymentHistory(Long loanId) {
        return paymentRepository.findByLoanId(loanId);
    }

    @Transactional
    public Map<String, Object> processBatchPayments(List<Payment> payments) {
        Map<String, Object> results = new HashMap<>();
        int processed = 0;
        int failed = 0;

        for (Payment payment : payments) {
            try {
                submitPayment(payment);
                processed++;
            } catch (Exception e) {
                failed++;
            }
        }

        results.put("totalSubmitted", payments.size());
        results.put("processed", processed);
        results.put("failed", failed);
        results.put("batchDate", new Date());
        return results;
    }

    public List<Payment> getPendingPayments() {
        return paymentRepository.findByStatus(PaymentStatus.PENDING);
    }
}
