package com.acme.autofinance.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.Instant;
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

    @JsonCreator
    public LoanFunded(
            @JsonProperty("eventId") String eventId,
            @JsonProperty("occurredAt") Instant occurredAt,
            @JsonProperty("aggregateId") String aggregateId,
            @JsonProperty("loanId") Long loanId,
            @JsonProperty("applicationNumber") String applicationNumber,
            @JsonProperty("fundingDate") LocalDate fundingDate,
            @JsonProperty("approvedAmount") BigDecimal approvedAmount,
            @JsonProperty("interestRate") BigDecimal interestRate,
            @JsonProperty("termMonths") Integer termMonths) {
        super(eventId, occurredAt, aggregateId);
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
