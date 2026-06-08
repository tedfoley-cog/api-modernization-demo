package com.acme.autofinance.events;

import java.math.BigDecimal;

/**
 * Published by the Payment context when payment processing fails (e.g. invalid
 * ACH routing number). No account or loan mutation occurs as a result.
 */
public class PaymentFailedEvent extends DomainEvent {

    private final Long loanId;
    private final Long paymentId;
    private final BigDecimal amount;
    private final String reason;

    public PaymentFailedEvent(Long loanId, Long paymentId, BigDecimal amount, String reason) {
        this.loanId = loanId;
        this.paymentId = paymentId;
        this.amount = amount;
        this.reason = reason;
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

    public String getReason() {
        return reason;
    }
}
