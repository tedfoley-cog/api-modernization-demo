package com.acme.payment.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Consumes domain events within the payment service boundary.
 * In production, downstream services (account-servicing, reporting)
 * would have their own consumers subscribing to these events.
 */
@Component
public class PaymentEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventConsumer.class);

    @EventListener
    public void onPaymentReceived(PaymentReceived event) {
        log.info("Event consumed: PaymentReceived — paymentId={}, loanId={}, amount={}",
                event.getPaymentId(), event.getLoanId(), event.getPaymentAmount());
    }

    @EventListener
    public void onPaymentAllocated(PaymentAllocated event) {
        log.info("Event consumed: PaymentAllocated — paymentId={}, principal={}, interest={}, fees={}",
                event.getPaymentId(), event.getPrincipalAmount(),
                event.getInterestAmount(), event.getFeeAmount());
    }

    @EventListener
    public void onPaymentProcessed(PaymentProcessed event) {
        log.info("Event consumed: PaymentProcessed — paymentId={}, loanId={}, status={}, totalPaid={}",
                event.getPaymentId(), event.getLoanId(), event.getStatus(), event.getTotalPaid());
    }

    @EventListener
    public void onLateFeesAssessed(LateFeesAssessed event) {
        log.info("Event consumed: LateFeesAssessed — loanId={}, fee={}, daysPastDue={}",
                event.getLoanId(), event.getFeeAmount(), event.getDaysPastDue());
    }
}
