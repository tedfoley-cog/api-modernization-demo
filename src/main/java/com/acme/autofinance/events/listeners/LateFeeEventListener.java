package com.acme.autofinance.events.listeners;

import com.acme.autofinance.events.LateFeeRequiredEvent;
import com.acme.autofinance.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Payment context reaction to a late-fee request raised by the Loan context
 * during end-of-day processing. Inverts the old synchronous
 * {@code LoanService -> PaymentService.assessLateFee()} call into an event.
 */
@Component
public class LateFeeEventListener {

    private final PaymentService paymentService;

    @Autowired
    public LateFeeEventListener(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @EventListener
    @Transactional
    public void on(LateFeeRequiredEvent event) {
        paymentService.assessLateFee(event.getLoanId(), event.getDaysPastDue());
    }
}
