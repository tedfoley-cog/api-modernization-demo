package com.acme.payment.event;

import java.math.BigDecimal;

/**
 * Emitted when a late fee is assessed against a loan. Downstream domains use this to
 * reflect the charge; the payment service no longer reaches into account data to do so.
 */
public final class LateFeesAssessed extends DomainEvent {

    private final Long paymentId;
    private final Long loanId;
    private final BigDecimal lateFee;
    private final int daysPastDue;

    public LateFeesAssessed(Long paymentId, Long loanId, BigDecimal lateFee, int daysPastDue) {
        super(String.valueOf(loanId));
        this.paymentId = paymentId;
        this.loanId = loanId;
        this.lateFee = lateFee;
        this.daysPastDue = daysPastDue;
    }

    @Override
    public String getEventType() {
        return "payment.late_fees_assessed";
    }

    public Long getPaymentId() { return paymentId; }
    public Long getLoanId() { return loanId; }
    public BigDecimal getLateFee() { return lateFee; }
    public int getDaysPastDue() { return daysPastDue; }
}
