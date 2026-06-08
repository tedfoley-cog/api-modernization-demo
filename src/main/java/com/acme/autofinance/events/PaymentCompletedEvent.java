package com.acme.autofinance.events;

import java.math.BigDecimal;

/**
 * Published by the Payment context when an ACH payment clears successfully.
 *
 * <p>Replaces the synchronous {@code updateAccountBalance()} and
 * {@code updateLoanAfterPayment()} side effects: the Account and Loan contexts
 * react to this event independently.
 */
public class PaymentCompletedEvent extends DomainEvent {

    private final Long loanId;
    private final Long paymentId;
    private final BigDecimal amount;
    private final BigDecimal principalAmount;

    public PaymentCompletedEvent(Long loanId, Long paymentId, BigDecimal amount, BigDecimal principalAmount) {
        this.loanId = loanId;
        this.paymentId = paymentId;
        this.amount = amount;
        this.principalAmount = principalAmount;
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

    public BigDecimal getPrincipalAmount() {
        return principalAmount;
    }
}
