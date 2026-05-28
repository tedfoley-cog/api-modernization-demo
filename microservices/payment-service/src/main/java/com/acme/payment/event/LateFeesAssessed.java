package com.acme.payment.event;

import java.math.BigDecimal;
import java.util.Date;

/**
 * Published when a late fee is assessed on a delinquent account.
 * Replaces the synchronous call from LoanService.runEndOfDayProcessing()
 * to PaymentService.assessLateFee() in the monolith.
 */
public class LateFeesAssessed extends DomainEvent {

    private final Long loanId;
    private final BigDecimal feeAmount;
    private final int daysPastDue;
    private final Date assessmentDate;

    public LateFeesAssessed(Long loanId, BigDecimal feeAmount,
                            int daysPastDue, Date assessmentDate) {
        super("LateFeesAssessed");
        this.loanId = loanId;
        this.feeAmount = feeAmount;
        this.daysPastDue = daysPastDue;
        this.assessmentDate = assessmentDate;
    }

    public Long getLoanId() { return loanId; }
    public BigDecimal getFeeAmount() { return feeAmount; }
    public int getDaysPastDue() { return daysPastDue; }
    public Date getAssessmentDate() { return assessmentDate; }
}
