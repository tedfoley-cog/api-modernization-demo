package com.acme.autofinance.service;

import com.acme.autofinance.event.LateFeeAssessedEvent;
import com.acme.autofinance.event.PaymentCompletedEvent;
import com.acme.autofinance.event.PaymentFailedEvent;
import com.acme.autofinance.event.PaymentReceivedEvent;
import com.acme.autofinance.model.LoanApplication;
import com.acme.autofinance.model.Payment;
import com.acme.autofinance.model.PaymentMethod;
import com.acme.autofinance.model.PaymentStatus;
import com.acme.autofinance.repository.AccountRepository;
import com.acme.autofinance.repository.LoanRepository;
import com.acme.autofinance.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Payment processing service — refactored to publish domain events instead of
 * synchronous cross-domain calls.
 *
 * BEFORE: submitPayment -> allocatePayment -> processAchPayment -> updateAccountBalance
 *         -> updateLoanAfterPayment (all synchronous, single @Transactional)
 *
 * AFTER:  submitPayment -> allocatePayment -> processAchPayment -> publish PaymentCompleted
 *         (Account + Loan updates handled by PaymentEventHandler asynchronously)
 */
@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    // Externalized late fee schedule (previously hardcoded)
    private static final BigDecimal LATE_FEE_FLAT = new BigDecimal("25.00");
    private static final BigDecimal LATE_FEE_PCT = new BigDecimal("5.00");
    private static final BigDecimal MAX_LATE_FEE = new BigDecimal("50.00");

    @Transactional
    public Payment submitPayment(Payment payment) {
        payment.setConfirmationNumber("PMT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setPaymentDate(new Date());

        // Validate the loan exists
        LoanApplication loan = loanRepository.findById(payment.getLoanId())
                .orElseThrow(() -> new RuntimeException("Loan not found: " + payment.getLoanId()));

        // Allocate payment: principal vs interest vs fees
        allocatePayment(payment, loan);

        Payment saved = paymentRepository.save(payment);

        // Publish PaymentReceived event (replaces synchronous cross-domain calls)
        PaymentReceivedEvent receivedEvent = new PaymentReceivedEvent(
                saved.getLoanId(), saved.getId(), saved.getPaymentAmount(),
                saved.getPrincipalAmount(), saved.getInterestAmount(),
                saved.getFeeAmount(), saved.getConfirmationNumber());
        eventPublisher.publishEvent(receivedEvent);
        log.info("Published PaymentReceivedEvent: loanId={}, amount={}, confirmation={}",
                saved.getLoanId(), saved.getPaymentAmount(), saved.getConfirmationNumber());

        // Process immediately if ACH
        if (payment.getPaymentMethod() == PaymentMethod.ACH) {
            processAchPayment(saved);
        }

        return saved;
    }

    private void allocatePayment(Payment payment, LoanApplication loan) {
        BigDecimal totalAmount = payment.getPaymentAmount();
        BigDecimal monthlyRate = BigDecimal.ZERO;
        if (loan.getInterestRate() != null) {
            monthlyRate = loan.getInterestRate()
                    .divide(new BigDecimal("1200"), 10, RoundingMode.HALF_UP);
        }

        // Interest portion: current balance * monthly rate
        Optional<com.acme.autofinance.model.Account> accountOpt =
                accountRepository.findByLoanId(loan.getId());
        BigDecimal currentBalance = accountOpt.isPresent()
                ? accountOpt.get().getCurrentBalance()
                : loan.getApprovedAmount();

        BigDecimal interestPortion = BigDecimal.ZERO;
        if (currentBalance != null) {
            interestPortion = currentBalance.multiply(monthlyRate).setScale(2, RoundingMode.HALF_UP);
        }

        // Late fees first, then interest, then principal
        BigDecimal feesPortion = BigDecimal.ZERO;
        BigDecimal remaining = totalAmount;

        List<Payment> pendingFees = paymentRepository.findByLoanIdAndStatus(
                loan.getId(), PaymentStatus.PENDING);
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

        // Validate routing/account numbers
        if (payment.getAchRoutingNumber() == null || payment.getAchRoutingNumber().length() != 9) {
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);

            // Publish PaymentFailed event so downstream consumers learn about the failure
            PaymentFailedEvent failedEvent = new PaymentFailedEvent(
                    payment.getLoanId(), payment.getId(),
                    "Invalid ACH routing number", payment.getConfirmationNumber());
            eventPublisher.publishEvent(failedEvent);
            log.info("Published PaymentFailedEvent: loanId={}, confirmation={}, reason={}",
                    payment.getLoanId(), payment.getConfirmationNumber(), "Invalid ACH routing number");
            return;
        }

        // Mark as completed
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setProcessedDate(new Date());
        paymentRepository.save(payment);

        // Publish PaymentCompleted event instead of synchronous updateAccountBalance + updateLoanAfterPayment
        BigDecimal totalPaid = paymentRepository.sumCompletedPayments(payment.getLoanId());
        PaymentCompletedEvent completedEvent = new PaymentCompletedEvent(
                payment.getLoanId(), payment.getId(),
                payment.getPrincipalAmount(), totalPaid, payment.getConfirmationNumber());
        eventPublisher.publishEvent(completedEvent);
        log.info("Published PaymentCompletedEvent: loanId={}, totalPaidToDate={}",
                payment.getLoanId(), totalPaid);
    }

    public void assessLateFee(Long loanId, int daysPastDue) {
        Optional<com.acme.autofinance.model.Account> accountOpt =
                accountRepository.findByLoanId(loanId);
        if (!accountOpt.isPresent()) return;

        com.acme.autofinance.model.Account account = accountOpt.get();

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

        String confirmationNumber = "FEE-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Payment feePayment = new Payment();
        feePayment.setLoanId(loanId);
        feePayment.setPaymentAmount(BigDecimal.ZERO);
        feePayment.setLateFee(lateFee);
        feePayment.setStatus(PaymentStatus.PENDING);
        feePayment.setPaymentDate(new Date());
        feePayment.setConfirmationNumber(confirmationNumber);
        paymentRepository.save(feePayment);

        // Publish LateFeeAssessed event
        LateFeeAssessedEvent event = new LateFeeAssessedEvent(
                loanId, lateFee, daysPastDue, confirmationNumber);
        eventPublisher.publishEvent(event);
        log.info("Published LateFeeAssessedEvent: loanId={}, fee={}, daysPastDue={}",
                loanId, lateFee, daysPastDue);
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
