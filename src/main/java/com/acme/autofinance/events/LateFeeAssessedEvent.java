package com.acme.autofinance.events;

import java.math.BigDecimal;

/**
 * Published by the Payment context after a late fee record has been created in
 * response to a {@link LateFeeRequiredEvent}.
 */
public class LateFeeAssessedEvent extends DomainEvent {

    private final Long loanId;
    private final Long paymentId;
    private final BigDecimal lateFee;
    private final int daysPastDue;

    public LateFeeAssessedEvent(Long loanId, Long paymentId, BigDecimal lateFee, int daysPastDue) {
        this.loanId = loanId;
        this.paymentId = paymentId;
        this.lateFee = lateFee;
        this.daysPastDue = daysPastDue;
    }

    public Long getLoanId() {
        return loanId;
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public BigDecimal getLateFee() {
        return lateFee;
    }

    public int getDaysPastDue() {
        return daysPastDue;
    }
}
