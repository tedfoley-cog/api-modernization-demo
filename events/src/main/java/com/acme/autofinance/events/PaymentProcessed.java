package com.acme.autofinance.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** Emitted when an ACH/EFT payment completes successfully. */
public final class PaymentProcessed extends DomainEvent {

    private final Long paymentId;
    private final Long loanId;
    private final LocalDate processedDate;
    private final String status;
    private final BigDecimal principalApplied;
    private final BigDecimal newBalance;

    public PaymentProcessed(Long paymentId, Long loanId, LocalDate processedDate,
                            String status, BigDecimal principalApplied, BigDecimal newBalance) {
        super(String.valueOf(paymentId));
        this.paymentId = paymentId;
        this.loanId = loanId;
        this.processedDate = processedDate;
        this.status = status;
        this.principalApplied = principalApplied;
        this.newBalance = newBalance;
    }

    @JsonCreator
    public PaymentProcessed(
            @JsonProperty("eventId") String eventId,
            @JsonProperty("occurredAt") Instant occurredAt,
            @JsonProperty("aggregateId") String aggregateId,
            @JsonProperty("paymentId") Long paymentId,
            @JsonProperty("loanId") Long loanId,
            @JsonProperty("processedDate") LocalDate processedDate,
            @JsonProperty("status") String status,
            @JsonProperty("principalApplied") BigDecimal principalApplied,
            @JsonProperty("newBalance") BigDecimal newBalance) {
        super(eventId, occurredAt, aggregateId);
        this.paymentId = paymentId;
        this.loanId = loanId;
        this.processedDate = processedDate;
        this.status = status;
        this.principalApplied = principalApplied;
        this.newBalance = newBalance;
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public Long getLoanId() {
        return loanId;
    }

    public LocalDate getProcessedDate() {
        return processedDate;
    }

    public String getStatus() {
        return status;
    }

    public BigDecimal getPrincipalApplied() {
        return principalApplied;
    }

    public BigDecimal getNewBalance() {
        return newBalance;
    }

    @Override
    public String eventType() {
        return "PaymentProcessed";
    }

    @Override
    public String topic() {
        return EventTopics.PAYMENT_PROCESSING;
    }
}
