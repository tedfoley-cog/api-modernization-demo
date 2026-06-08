package com.acme.autofinance.service;

import com.acme.autofinance.events.DomainEventPublisher;
import com.acme.autofinance.events.LateFeeAssessedEvent;
import com.acme.autofinance.events.PaymentCompletedEvent;
import com.acme.autofinance.events.PaymentFailedEvent;
import com.acme.autofinance.events.PaymentSubmittedEvent;
import com.acme.autofinance.events.port.LoanReference;
import com.acme.autofinance.events.port.LoanValidationPort;
import com.acme.autofinance.model.Payment;
import com.acme.autofinance.model.PaymentMethod;
import com.acme.autofinance.model.PaymentStatus;
import com.acme.autofinance.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
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
 * Payment processing service — extracted as an event-driven bounded context.
 *
 * <p>This service no longer reaches into the Loan or Account contexts' data:
 * the only cross-domain reads go through {@link LoanValidationPort} (an
 * anti-corruption layer), and all cross-domain writes are emitted as domain
 * events ({@link PaymentCompletedEvent}, {@link PaymentFailedEvent},
 * {@link LateFeeAssessedEvent}) for the Account and Loan contexts to react to.
 */
@Service
public class PaymentService {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private LoanValidationPort loanValidationPort;

    @Autowired
    private DomainEventPublisher eventPublisher;

    // Hardcoded late fee schedule — should be externalized configuration
    private static final BigDecimal LATE_FEE_FLAT = new BigDecimal("25.00");
    private static final BigDecimal LATE_FEE_PCT = new BigDecimal("5.00");
    private static final BigDecimal MAX_LATE_FEE = new BigDecimal("50.00");

    @Transactional
    public Payment submitPayment(Payment payment) {
        payment.setConfirmationNumber("PMT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setPaymentDate(new Date());

        // Validate the loan exists via the anti-corruption layer — no direct
        // dependency on the Loan/Account contexts' repositories.
        LoanReference loanRef = loanValidationPort.findLoanReference(payment.getLoanId())
                .orElseThrow(() -> new RuntimeException("Loan not found: " + payment.getLoanId()));

        // Allocate payment: principal vs interest vs fees — inline business logic
        allocatePayment(payment, loanRef);

        Payment saved = paymentRepository.save(payment);

        // Announce the submission for any interested context.
        eventPublisher.publish(new PaymentSubmittedEvent(
                saved.getLoanId(), saved.getId(), saved.getPaymentAmount(), saved.getConfirmationNumber()));

        // Process immediately if ACH — synchronous processing
        if (payment.getPaymentMethod() == PaymentMethod.ACH) {
            processAchPayment(saved);
        }

        return saved;
    }

    private void allocatePayment(Payment payment, LoanReference loanRef) {
        BigDecimal totalAmount = payment.getPaymentAmount();
        BigDecimal monthlyRate = BigDecimal.ZERO;
        if (loanRef.getInterestRate() != null) {
            monthlyRate = loanRef.getInterestRate()
                    .divide(new BigDecimal("1200"), 10, RoundingMode.HALF_UP);
        }

        // Interest portion: current balance * monthly rate
        BigDecimal currentBalance = loanRef.getCurrentBalance() != null
                ? loanRef.getCurrentBalance()
                : loanRef.getApprovedAmount();

        BigDecimal interestPortion = BigDecimal.ZERO;
        if (currentBalance != null) {
            interestPortion = currentBalance.multiply(monthlyRate).setScale(2, RoundingMode.HALF_UP);
        }

        // Late fees first, then interest, then principal — hardcoded allocation order
        BigDecimal feesPortion = BigDecimal.ZERO;
        BigDecimal remaining = totalAmount;

        // Apply any outstanding late fees first
        List<Payment> pendingFees = paymentRepository.findByLoanIdAndStatus(
                payment.getLoanId(), PaymentStatus.PENDING);
        for (Payment fee : pendingFees) {
            if (fee.getLateFee() != null && fee.getLateFee().compareTo(BigDecimal.ZERO) > 0) {
                feesPortion = feesPortion.add(fee.getLateFee());
            }
        }

        if (feesPortion.compareTo(remaining) > 0) {
            feesPortion = remaining;
        }
        remaining = remaining.subtract(feesPortion);

        // Then interest
        if (interestPortion.compareTo(remaining) > 0) {
            interestPortion = remaining;
        }
        remaining = remaining.subtract(interestPortion);

        // Rest goes to principal
        BigDecimal principalPortion = remaining;

        payment.setFeeAmount(feesPortion);
        payment.setInterestAmount(interestPortion);
        payment.setPrincipalAmount(principalPortion);
    }

    @Transactional
    public void processAchPayment(Payment payment) {
        // Simulate ACH processing — in production this calls external ACH processor
        payment.setStatus(PaymentStatus.PROCESSING);
        paymentRepository.save(payment);

        // Validate routing/account numbers — inline
        if (payment.getAchRoutingNumber() == null || payment.getAchRoutingNumber().length() != 9) {
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            // Publish failure — no account/loan side effects occur.
            eventPublisher.publish(new PaymentFailedEvent(
                    payment.getLoanId(), payment.getId(), payment.getPaymentAmount(),
                    "Invalid ACH routing number"));
            return;
        }

        // Mark as completed
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setProcessedDate(new Date());
        paymentRepository.save(payment);

        // Publish completion instead of synchronously updating account & loan.
        // The Account and Loan contexts react via their event listeners.
        eventPublisher.publish(new PaymentCompletedEvent(
                payment.getLoanId(), payment.getId(),
                payment.getPaymentAmount(), payment.getPrincipalAmount()));
    }

    @Transactional
    public void assessLateFee(Long loanId, int daysPastDue) {
        // Use the anti-corruption read model for the loan's current balance.
        Optional<LoanReference> loanRefOpt = loanValidationPort.findLoanReference(loanId);
        if (!loanRefOpt.isPresent()) {
            return;
        }
        BigDecimal currentBalance = loanRefOpt.get().getCurrentBalance();
        if (currentBalance == null) {
            return;
        }

        // Calculate late fee — hardcoded business rules
        BigDecimal lateFee;
        if (daysPastDue <= 30) {
            lateFee = LATE_FEE_FLAT;
        } else {
            // Percentage-based for >30 days
            lateFee = currentBalance
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

        eventPublisher.publish(new LateFeeAssessedEvent(loanId, saved.getId(), lateFee, daysPastDue));
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
