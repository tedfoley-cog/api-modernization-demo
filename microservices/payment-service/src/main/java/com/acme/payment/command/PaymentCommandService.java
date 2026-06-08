package com.acme.payment.command;

import com.acme.payment.domain.Payment;
import com.acme.payment.domain.PaymentAllocation;
import com.acme.payment.event.EventPublisher;
import com.acme.payment.event.LateFeesAssessed;
import com.acme.payment.event.PaymentAllocated;
import com.acme.payment.event.PaymentProcessed;
import com.acme.payment.event.PaymentReceived;
import com.acme.payment.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Command (write) side handler. Submits, allocates and processes payments and
 * publishes domain events. It performs <strong>no</strong> synchronous calls
 * into the loan or account domains — downstream effects are driven by the
 * events it emits.
 */
@Service
public class PaymentCommandService {

    private final PaymentRepository paymentRepository;
    private final EventPublisher eventPublisher;

    public PaymentCommandService(PaymentRepository paymentRepository, EventPublisher eventPublisher) {
        this.paymentRepository = paymentRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Payment submit(SubmitPaymentCommand command) {
        Payment payment = Payment.submit(
                command.getLoanId(),
                command.getPaymentAmount(),
                command.getPaymentMethod(),
                command.getAchRoutingNumber(),
                command.getAchAccountNumber());

        // Allocation uses only values supplied on the command.
        PaymentAllocation allocation = payment.allocate(
                command.getOutstandingBalance(),
                command.getMonthlyInterestRate(),
                command.getOutstandingFees());

        Payment saved = paymentRepository.save(payment);

        eventPublisher.publish(new PaymentReceived(
                saved.getId(), saved.getLoanId(), saved.getPaymentAmount(),
                saved.getPaymentMethod(), saved.getConfirmationNumber()));

        eventPublisher.publish(new PaymentAllocated(
                saved.getId(), saved.getLoanId(),
                allocation.getPrincipalAmount(), allocation.getInterestAmount(), allocation.getFeeAmount()));

        if (saved.isAch()) {
            boolean settled = saved.settleAch();
            saved = paymentRepository.save(saved);

            // Only a successful settlement yields a PaymentProcessed event; a
            // failed ACH leaves the payment FAILED with no processedDate, which
            // would violate the PaymentProcessed contract (processedDate is required).
            if (settled) {
                BigDecimal newBalance = computeNewBalance(command.getOutstandingBalance(), saved.getPrincipalAmount());
                eventPublisher.publish(new PaymentProcessed(
                        saved.getId(), saved.getLoanId(), saved.getProcessedDate(),
                        saved.getStatus(), saved.getPrincipalAmount(), newBalance));
            }
        }

        return saved;
    }

    @Transactional
    public Payment assessLateFee(AssessLateFeeCommand command) {
        Payment fee = Payment.lateFeeCharge(
                command.getLoanId(), command.getOutstandingBalance(), command.getDaysPastDue());
        Payment saved = paymentRepository.save(fee);

        eventPublisher.publish(new LateFeesAssessed(
                saved.getLoanId(), saved.getLateFee(), command.getDaysPastDue(), saved.getPaymentDate()));

        return saved;
    }

    @Transactional
    public BatchResult processBatch(ProcessBatchCommand command) {
        int processed = 0;
        int failed = 0;
        for (SubmitPaymentCommand sub : command.getPayments()) {
            try {
                submit(sub);
                processed++;
            } catch (RuntimeException e) {
                failed++;
            }
        }
        return new BatchResult(command.getPayments().size(), processed, failed);
    }

    private BigDecimal computeNewBalance(BigDecimal outstandingBalance, BigDecimal principalApplied) {
        if (outstandingBalance == null) {
            return null;
        }
        BigDecimal applied = principalApplied == null ? BigDecimal.ZERO : principalApplied;
        BigDecimal newBalance = outstandingBalance.subtract(applied);
        return newBalance.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : newBalance;
    }
}
