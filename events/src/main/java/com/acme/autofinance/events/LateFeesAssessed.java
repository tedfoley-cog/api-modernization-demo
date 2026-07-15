package com.acme.autofinance.events;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Emitted when an account is past due and a late fee is charged. */
public final class LateFeesAssessed extends DomainEvent {

    private final Long loanId;
    private final BigDecimal feeAmount;
    private final Integer daysPastDue;
    private final LocalDate assessmentDate;

    public LateFeesAssessed(Long loanId, BigDecimal feeAmount, Integer daysPastDue, LocalDate assessmentDate) {
        super(String.valueOf(loanId));
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

    public Integer getDaysPastDue() {
        return daysPastDue;
    }

    public LocalDate getAssessmentDate() {
        return assessmentDate;
    }

    @Override
    public String eventType() {
        return "LateFeesAssessed";
    }

    @Override
    public String topic() {
        return EventTopics.PAYMENT_PROCESSING;
    }
}
