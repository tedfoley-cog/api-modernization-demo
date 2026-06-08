package com.acme.payment.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Example downstream consumer. It subscribes to payment domain events and reacts
 * (here, just logging) — demonstrating that reactions are decoupled. In the
 * monolith this work happened via synchronous calls into the account/loan
 * services; now it is driven entirely by published events.
 */
@Component
public class PaymentEventLogger {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventLogger.class);

    @EventListener
    public void on(PaymentReceived event) {
        log.info("[event] PaymentReceived id={} payment={} loan={} amount={} method={} confirmation={}",
                event.getEventId(), event.getPaymentId(), event.getLoanId(),
                event.getPaymentAmount(), event.getPaymentMethod(), event.getConfirmationNumber());
    }

    @EventListener
    public void on(PaymentAllocated event) {
        log.info("[event] PaymentAllocated id={} payment={} loan={} principal={} interest={} fee={}",
                event.getEventId(), event.getPaymentId(), event.getLoanId(),
                event.getPrincipalAmount(), event.getInterestAmount(), event.getFeeAmount());
    }

    @EventListener
    public void on(PaymentProcessed event) {
        log.info("[event] PaymentProcessed id={} payment={} loan={} status={} principalApplied={} newBalance={}",
                event.getEventId(), event.getPaymentId(), event.getLoanId(),
                event.getStatus(), event.getPrincipalApplied(), event.getNewBalance());
    }

    @EventListener
    public void on(LateFeesAssessed event) {
        log.info("[event] LateFeesAssessed id={} loan={} fee={} daysPastDue={}",
                event.getEventId(), event.getLoanId(), event.getFeeAmount(), event.getDaysPastDue());
    }
}
