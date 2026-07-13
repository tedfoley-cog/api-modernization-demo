package com.acme.payment.saga;

import com.acme.payment.domain.event.PaymentCompleted;
import com.acme.payment.domain.event.PaymentFailed;
import com.acme.payment.domain.model.Payment;
import com.acme.payment.domain.model.PaymentStatus;
import com.acme.payment.domain.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Saga orchestrator for distributed payment processing.
 *
 * Replaces the monolith's single @Transactional spanning Payment + Account + Loan tables.
 * In production, this would coordinate with Account Service and Loan Service via Kafka,
 * implementing compensating transactions on failure.
 *
 * Saga steps:
 *   1. Payment marked PROCESSING (Payment DB) -- already done by PaymentCommandService
 *   2. PaymentReceived event published
 *   3. Account Service: update balance (Account DB)
 *      - On failure: publish AccountUpdateFailed -> compensate
 *   4. Loan Service: check payoff (Loan DB)
 *      - On failure: publish LoanUpdateFailed -> compensate
 *
 * Compensation:
 *   - AccountUpdateFailed -> reverse payment to FAILED, no balance change
 *   - LoanUpdateFailed -> reverse payment to FAILED, restore account balance
 */
@Component
public class PaymentSagaOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(PaymentSagaOrchestrator.class);

    private final PaymentRepository paymentRepository;

    public PaymentSagaOrchestrator(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @EventListener
    public void onPaymentCompleted(PaymentCompleted event) {
        log.info("Saga: PaymentCompleted received for loanId={}, paymentId={}, totalPaid={}",
                event.getLoanId(), event.getPaymentId(), event.getTotalPaidToDate());

        // In production: send command to Account Service via Kafka
        // Account Service would:
        //   1. Subtract principalAmount from current balance
        //   2. Reset daysPastDue to 0
        //   3. Publish AccountBalanceUpdated event
        // If Account Service fails, it publishes AccountUpdateFailed,
        // and this saga would compensate by reversing the payment.

        log.info("Saga: would notify Account Service to update balance for loanId={}, principal={}",
                event.getLoanId(), event.getPrincipalAmount());
        log.info("Saga: would notify Loan Service to check payoff for loanId={}, totalPaid={}",
                event.getLoanId(), event.getTotalPaidToDate());
    }

    @EventListener
    @Transactional
    public void onPaymentFailed(PaymentFailed event) {
        log.warn("Saga: PaymentFailed for loanId={}, reason={}", event.getLoanId(), event.getReason());

        // Compensation: no balance update needed since the payment was never applied.
        // In production: publish PaymentReversed event so Account Service can
        // undo any partial balance changes.
    }

    /**
     * Compensation handler for when Account Service fails to update balance.
     * In production this would listen to AccountUpdateFailed events from Kafka.
     */
    public void compensateAccountUpdateFailure(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId).orElse(null);
        if (payment != null) {
            log.warn("Saga compensation: reversing payment {} due to account update failure", paymentId);
            payment.setStatus(PaymentStatus.REVERSED);
            paymentRepository.save(payment);
        }
    }
}
