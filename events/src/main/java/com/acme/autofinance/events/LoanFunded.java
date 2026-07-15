package com.acme.autofinance.events;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Emitted when a loan moves to FUNDED status. */
public final class LoanFunded extends DomainEvent {

    private final Long loanId;
    private final String applicationNumber;
    private final LocalDate fundingDate;
    private final BigDecimal approvedAmount;
    private final BigDecimal interestRate;
    private final Integer termMonths;

    public LoanFunded(Long loanId, String applicationNumber, LocalDate fundingDate,
                      BigDecimal approvedAmount, BigDecimal interestRate, Integer termMonths) {
        super(String.valueOf(loanId));
        this.loanId = loanId;
        this.applicationNumber = applicationNumber;
        this.fundingDate = fundingDate;
        this.approvedAmount = approvedAmount;
        this.interestRate = interestRate;
        this.termMonths = termMonths;
    }

    public Long getLoanId() {
        return loanId;
    }

    public String getApplicationNumber() {
        return applicationNumber;
    }

    public LocalDate getFundingDate() {
        return fundingDate;
    }

    public BigDecimal getApprovedAmount() {
        return approvedAmount;
    }

    public BigDecimal getInterestRate() {
        return interestRate;
    }

    public Integer getTermMonths() {
        return termMonths;
    }

    @Override
    public String eventType() {
        return "LoanFunded";
    }

    @Override
    public String topic() {
        return EventTopics.LOAN_ORIGINATION;
    }
}
