package com.acme.payment.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Example in-service consumer that reacts to the payment context's own events.
 *
 * <p>It demonstrates the consumer side of the decoupled flow: in a real deployment the
 * account-servicing and reporting services would subscribe to these same events off the
 * bus to update their own read models, instead of the monolith mutating their tables
 * synchronously.
 */
@Component
public class PaymentEventLogger {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventLogger.class);

    @EventListener
    public void on(PaymentReceived event) {
        log.info("[{}] PaymentReceived payment={} loan={} amount={}",
                event.getEventId(), event.getPaymentId(), event.getLoanId(), event.getPaymentAmount());
    }

    @EventListener
    public void on(PaymentAllocated event) {
        log.info("[{}] PaymentAllocated payment={} principal={} interest={} fees={}",
                event.getEventId(), event.getPaymentId(), event.getPrincipalAmount(),
                event.getInterestAmount(), event.getFeeAmount());
    }

    @EventListener
    public void on(PaymentProcessed event) {
        log.info("[{}] PaymentProcessed payment={} status={} principalApplied={}",
                event.getEventId(), event.getPaymentId(), event.getStatus(), event.getPrincipalApplied());
    }

    @EventListener
    public void on(LateFeesAssessed event) {
        log.info("[{}] LateFeesAssessed loan={} fee={} daysPastDue={}",
                event.getEventId(), event.getLoanId(), event.getFeeAmount(), event.getDaysPastDue());
    }
}
