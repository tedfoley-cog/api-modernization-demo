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
import java.math.RoundingMode;

/**
 * Write side of the Payment Processing CQRS split.
 *
 * <p>Each state change persists the aggregate and publishes a domain event instead of
 * synchronously mutating the loan and account domains the way the monolith's
 * {@code PaymentService} did. Downstream contexts react to the events on their own.
 */
@Service
public class PaymentCommandService {

    private static final BigDecimal LATE_FEE_FLAT = new BigDecimal("25.00");
    private static final BigDecimal LATE_FEE_PCT = new BigDecimal("5.00");
    private static final BigDecimal MAX_LATE_FEE = new BigDecimal("50.00");

    private final PaymentRepository paymentRepository;
    private final EventPublisher events;

    public PaymentCommandService(PaymentRepository paymentRepository, EventPublisher events) {
        this.paymentRepository = paymentRepository;
        this.events = events;
    }

    @Transactional
    public Payment submitPayment(SubmitPaymentCommand command) {
        Payment payment = Payment.received(
                command.getLoanId(),
                command.getPaymentAmount(),
                command.getPaymentMethod(),
                command.getAchRoutingNumber());

        PaymentAllocation allocation = payment.allocate(
                command.getOutstandingBalance(),
                command.getAnnualInterestRate(),
                command.getOutstandingFees());

        payment = paymentRepository.save(payment);

        events.publish(new PaymentReceived(
                payment.getId(), payment.getLoanId(), payment.getPaymentAmount(),
                payment.getPaymentMethod(), payment.getConfirmationNumber()));

        events.publish(new PaymentAllocated(
                payment.getId(), payment.getLoanId(),
                allocation.getPrincipalAmount(), allocation.getInterestAmount(), allocation.getFeeAmount()));

        // ACH/EFT clear straight through; other methods settle out of band.
        if (payment.isElectronic()) {
            processPayment(payment);
        }

        return payment;
    }

    @Transactional
    public Payment processPayment(Payment payment) {
        payment.beginProcessing();
        paymentRepository.save(payment);

        if (payment.isAch() && !payment.hasValidAchRouting()) {
            payment.fail();
            Payment failed = paymentRepository.save(payment);
            events.publish(new PaymentProcessed(
                    failed.getId(), failed.getLoanId(), failed.getStatus(), BigDecimal.ZERO));
            return failed;
        }

        payment.complete();
        Payment completed = paymentRepository.save(payment);

        events.publish(new PaymentProcessed(
                completed.getId(), completed.getLoanId(), completed.getStatus(),
                completed.getAllocation().getPrincipalAmount()));

        return completed;
    }

    @Transactional
    public Payment assessLateFee(AssessLateFeeCommand command) {
        BigDecimal fee;
        if (command.getDaysPastDue() <= 30) {
            fee = LATE_FEE_FLAT;
        } else {
            fee = command.getCurrentBalance()
                    .multiply(LATE_FEE_PCT)
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            if (fee.compareTo(MAX_LATE_FEE) > 0) {
                fee = MAX_LATE_FEE;
            }
        }

        Payment feeCharge = paymentRepository.save(Payment.lateFeeCharge(command.getLoanId(), fee));

        events.publish(new LateFeesAssessed(
                feeCharge.getId(), feeCharge.getLoanId(), fee, command.getDaysPastDue()));

        return feeCharge;
    }
}
