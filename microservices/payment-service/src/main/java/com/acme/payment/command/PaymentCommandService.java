package com.acme.payment.command;

import com.acme.payment.domain.Payment;
import com.acme.payment.domain.PaymentAllocation;
import com.acme.payment.domain.PaymentAllocator;
import com.acme.payment.domain.PaymentMethod;
import com.acme.payment.domain.PaymentStatus;
import com.acme.payment.event.EventPublisher;
import com.acme.payment.event.LateFeesAssessed;
import com.acme.payment.event.PaymentAllocated;
import com.acme.payment.event.PaymentProcessed;
import com.acme.payment.event.PaymentReceived;
import com.acme.payment.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Write side of the CQRS split. Handles SubmitPayment / ProcessBatch / AssessLateFee
 * commands and preserves the monolith's business logic, but replaces every synchronous
 * cross-domain side effect with a published domain event.
 *
 * <p>What used to happen inline in the monolith now becomes an event:
 * <ul>
 *   <li>{@code updateAccountBalance(...)} &rarr; consumers of {@link PaymentProcessed}</li>
 *   <li>{@code updateLoanAfterPayment(...)} / {@code accountService.closeAccount(...)}
 *       &rarr; consumers of {@link PaymentProcessed} (payoff decision lives downstream)</li>
 * </ul>
 */
@Service
public class PaymentCommandService {

    // Late fee schedule, preserved from the monolith.
    private static final BigDecimal LATE_FEE_FLAT = new BigDecimal("25.00");
    private static final BigDecimal LATE_FEE_PCT = new BigDecimal("5.00");
    private static final BigDecimal MAX_LATE_FEE = new BigDecimal("50.00");

    private final PaymentRepository paymentRepository;
    private final EventPublisher eventPublisher;

    public PaymentCommandService(PaymentRepository paymentRepository, EventPublisher eventPublisher) {
        this.paymentRepository = paymentRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Payment submitPayment(SubmitPaymentCommand command) {
        validate(command);

        Payment payment = new Payment();
        payment.setLoanId(command.getLoanId());
        payment.setPaymentAmount(command.getPaymentAmount());
        payment.setPaymentMethod(command.getPaymentMethod());
        payment.setAchRoutingNumber(command.getAchRoutingNumber());
        payment.setAchAccountNumber(command.getAchAccountNumber());
        payment.setConfirmationNumber("PMT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setPaymentDate(new Date());

        // Allocation: late fees -> interest -> principal. Cross-domain context (balance,
        // rate) comes from the command, not from loan/account repositories.
        BigDecimal outstandingLateFees = sumOutstandingLateFees(command.getLoanId());
        PaymentAllocation allocation = PaymentAllocator.allocate(
                command.getPaymentAmount(),
                command.getOutstandingBalance(),
                command.getAnnualInterestRate(),
                outstandingLateFees);
        payment.setFeeAmount(allocation.getFeeAmount());
        payment.setInterestAmount(allocation.getInterestAmount());
        payment.setPrincipalAmount(allocation.getPrincipalAmount());

        Payment saved = paymentRepository.save(payment);

        // Instead of synchronously mutating loan/account state, publish facts.
        eventPublisher.publish(new PaymentReceived(saved.getId(), saved.getLoanId(),
                saved.getPaymentAmount(), saved.getPaymentMethod(), saved.getConfirmationNumber()));
        eventPublisher.publish(new PaymentAllocated(saved.getId(), saved.getLoanId(),
                saved.getFeeAmount(), saved.getInterestAmount(), saved.getPrincipalAmount()));

        // ACH is processed immediately, mirroring the monolith.
        if (saved.getPaymentMethod() == PaymentMethod.ACH) {
            processAchPayment(saved);
        }

        return saved;
    }

    /**
     * ACH processing. On success a {@link PaymentProcessed} event carries the principal
     * applied and the completed-to-date total, which downstream domains use to update the
     * account balance and decide on loan payoff / account closure.
     */
    @Transactional
    public void processAchPayment(Payment payment) {
        payment.setStatus(PaymentStatus.PROCESSING);
        paymentRepository.save(payment);

        // ACH routing number must be exactly 9 digits.
        if (payment.getAchRoutingNumber() == null || payment.getAchRoutingNumber().length() != 9) {
            payment.setStatus(PaymentStatus.FAILED);
            Payment failed = paymentRepository.save(payment);
            eventPublisher.publish(new PaymentProcessed(failed.getId(), failed.getLoanId(),
                    failed.getStatus(), BigDecimal.ZERO, sumCompleted(failed.getLoanId())));
            return;
        }

        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setProcessedDate(new Date());
        Payment completed = paymentRepository.save(payment);

        eventPublisher.publish(new PaymentProcessed(completed.getId(), completed.getLoanId(),
                completed.getStatus(), completed.getPrincipalAmount(), sumCompleted(completed.getLoanId())));
    }

    @Transactional
    public BatchResult processBatch(ProcessBatchCommand command) {
        int processed = 0;
        int failed = 0;
        for (SubmitPaymentCommand sub : command.getPayments()) {
            try {
                submitPayment(sub);
                processed++;
            } catch (RuntimeException e) {
                failed++;
            }
        }
        return new BatchResult(command.getPayments().size(), processed, failed);
    }

    /**
     * Assess a late fee, preserving the monolith schedule:
     * flat $25 for &le;30 days past due, otherwise 5% of the outstanding balance capped at $50.
     */
    @Transactional
    public Payment assessLateFee(AssessLateFeeCommand command) {
        BigDecimal lateFee;
        if (command.getDaysPastDue() <= 30) {
            lateFee = LATE_FEE_FLAT;
        } else {
            BigDecimal balance = command.getOutstandingBalance() == null
                    ? BigDecimal.ZERO : command.getOutstandingBalance();
            lateFee = balance.multiply(LATE_FEE_PCT)
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            if (lateFee.compareTo(MAX_LATE_FEE) > 0) {
                lateFee = MAX_LATE_FEE;
            }
        }

        Payment feePayment = new Payment();
        feePayment.setLoanId(command.getLoanId());
        feePayment.setPaymentAmount(BigDecimal.ZERO);
        feePayment.setLateFee(lateFee);
        feePayment.setStatus(PaymentStatus.PENDING);
        feePayment.setPaymentDate(new Date());
        feePayment.setConfirmationNumber("FEE-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        Payment saved = paymentRepository.save(feePayment);

        eventPublisher.publish(new LateFeesAssessed(saved.getId(), saved.getLoanId(),
                lateFee, command.getDaysPastDue()));

        return saved;
    }

    private BigDecimal sumOutstandingLateFees(Long loanId) {
        BigDecimal fees = BigDecimal.ZERO;
        List<Payment> pendingFees = paymentRepository.findByLoanIdAndStatus(loanId, PaymentStatus.PENDING);
        for (Payment fee : pendingFees) {
            if (fee.getLateFee() != null && fee.getLateFee().compareTo(BigDecimal.ZERO) > 0) {
                fees = fees.add(fee.getLateFee());
            }
        }
        return fees;
    }

    private BigDecimal sumCompleted(Long loanId) {
        BigDecimal total = paymentRepository.sumCompletedPayments(loanId);
        return total == null ? BigDecimal.ZERO : total;
    }

    private void validate(SubmitPaymentCommand command) {
        if (command.getLoanId() == null) {
            throw new InvalidPaymentException("Loan ID is required");
        }
        if (command.getPaymentAmount() == null
                || command.getPaymentAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidPaymentException("Payment amount must be positive");
        }
    }
}
