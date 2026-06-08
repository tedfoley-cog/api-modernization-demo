package com.acme.payment.event;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Emitted when a late fee is assessed against a loan. Field names form part of
 * the published event contract.
 */
public final class LateFeesAssessed extends DomainEvent {

    private final Long loanId;
    private final BigDecimal feeAmount;
    private final int daysPastDue;
    private final Instant assessmentDate;

    public LateFeesAssessed(Long loanId,
                            BigDecimal feeAmount,
                            int daysPastDue,
                            Instant assessmentDate) {
        super("LateFeesAssessed", 1);
        this.loanId = loanId;
        this.feeAmount = feeAmount;
        this.daysPastDue = daysPastDue;
        this.assessmentDate = assessmentDate;
    }

    public Long getLoanId() {
        return loanId;
    }

    public BigDecimal getFeeAmount() {
        return feeAmount;
    }

    public int getDaysPastDue() {
        return daysPastDue;
    }

    public Instant getAssessmentDate() {
        return assessmentDate;
    }
}
