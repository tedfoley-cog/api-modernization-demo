package com.acme.autofinance.events;

import java.math.BigDecimal;

/**
 * Published by the Payment context when a payment is received and persisted
 * (before downstream processing). Carries the reference data other contexts
 * need without exposing the {@code Payment} aggregate itself.
 */
public class PaymentSubmittedEvent extends DomainEvent {

    private final Long loanId;
    private final Long paymentId;
    private final BigDecimal amount;
    private final String confirmationNumber;

    public PaymentSubmittedEvent(Long loanId, Long paymentId, BigDecimal amount, String confirmationNumber) {
        this.loanId = loanId;
        this.paymentId = paymentId;
        this.amount = amount;
        this.confirmationNumber = confirmationNumber;
    }

    public Long getLoanId() {
        return loanId;
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getConfirmationNumber() {
        return confirmationNumber;
    }
}
