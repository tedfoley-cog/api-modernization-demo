package com.acme.autofinance.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.Instant;

/** Emitted when a credit check is completed (approved or declined). */
public final class CreditDecisionMade extends DomainEvent {

    private final Long applicationId;
    private final String decision;
    private final Integer creditScore;
    private final String riskTier;
    private final BigDecimal approvedAmount;
    private final BigDecimal offeredRate;

    public CreditDecisionMade(Long applicationId, String decision, Integer creditScore,
                              String riskTier, BigDecimal approvedAmount, BigDecimal offeredRate) {
        super(String.valueOf(applicationId));
        this.applicationId = applicationId;
        this.decision = decision;
        this.creditScore = creditScore;
        this.riskTier = riskTier;
        this.approvedAmount = approvedAmount;
        this.offeredRate = offeredRate;
    }

    @JsonCreator
    public CreditDecisionMade(
            @JsonProperty("eventId") String eventId,
            @JsonProperty("occurredAt") Instant occurredAt,
            @JsonProperty("aggregateId") String aggregateId,
            @JsonProperty("applicationId") Long applicationId,
            @JsonProperty("decision") String decision,
            @JsonProperty("creditScore") Integer creditScore,
            @JsonProperty("riskTier") String riskTier,
            @JsonProperty("approvedAmount") BigDecimal approvedAmount,
            @JsonProperty("offeredRate") BigDecimal offeredRate) {
        super(eventId, occurredAt, aggregateId);
        this.applicationId = applicationId;
        this.decision = decision;
        this.creditScore = creditScore;
        this.riskTier = riskTier;
        this.approvedAmount = approvedAmount;
        this.offeredRate = offeredRate;
    }

    public Long getApplicationId() {
        return applicationId;
    }

    public String getDecision() {
        return decision;
    }

    public Integer getCreditScore() {
        return creditScore;
    }

    public String getRiskTier() {
        return riskTier;
    }

    public BigDecimal getApprovedAmount() {
        return approvedAmount;
    }

    public BigDecimal getOfferedRate() {
        return offeredRate;
    }

    @Override
    public String eventType() {
        return "CreditDecisionMade";
    }

    @Override
    public String topic() {
        return EventTopics.LOAN_ORIGINATION;
    }
}
