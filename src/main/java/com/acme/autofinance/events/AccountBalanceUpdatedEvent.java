package com.acme.autofinance.events;

import java.math.BigDecimal;

/**
 * Published by the Account context after it applies a payment to an account
 * balance (in reaction to a {@link PaymentCompletedEvent}). Allows other
 * contexts (e.g. Reporting) to react to balance changes asynchronously.
 */
public class AccountBalanceUpdatedEvent extends DomainEvent {

    private final Long loanId;
    private final Long paymentId;
    private final BigDecimal newBalance;

    public AccountBalanceUpdatedEvent(Long loanId, Long paymentId, BigDecimal newBalance) {
        this.loanId = loanId;
        this.paymentId = paymentId;
        this.newBalance = newBalance;
    }

    public Long getLoanId() {
        return loanId;
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public BigDecimal getNewBalance() {
        return newBalance;
    }
}
