package com.acme.autofinance.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.Instant;

/** Emitted when a payment is submitted by a borrower. */
public final class PaymentReceived extends DomainEvent {

    private final Long paymentId;
    private final Long loanId;
    private final BigDecimal paymentAmount;
    private final String paymentMethod;
    private final String confirmationNumber;

    public PaymentReceived(Long paymentId, Long loanId, BigDecimal paymentAmount,
                           String paymentMethod, String confirmationNumber) {
        super(String.valueOf(paymentId));
        this.paymentId = paymentId;
        this.loanId = loanId;
        this.paymentAmount = paymentAmount;
        this.paymentMethod = paymentMethod;
        this.confirmationNumber = confirmationNumber;
    }

    @JsonCreator
    public PaymentReceived(
            @JsonProperty("eventId") String eventId,
            @JsonProperty("occurredAt") Instant occurredAt,
            @JsonProperty("aggregateId") String aggregateId,
            @JsonProperty("paymentId") Long paymentId,
            @JsonProperty("loanId") Long loanId,
            @JsonProperty("paymentAmount") BigDecimal paymentAmount,
            @JsonProperty("paymentMethod") String paymentMethod,
            @JsonProperty("confirmationNumber") String confirmationNumber) {
        super(eventId, occurredAt, aggregateId);
        this.paymentId = paymentId;
        this.loanId = loanId;
        this.paymentAmount = paymentAmount;
        this.paymentMethod = paymentMethod;
        this.confirmationNumber = confirmationNumber;
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public Long getLoanId() {
        return loanId;
    }

    public BigDecimal getPaymentAmount() {
        return paymentAmount;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public String getConfirmationNumber() {
        return confirmationNumber;
    }

    @Override
    public String eventType() {
        return "PaymentReceived";
    }

    @Override
    public String topic() {
        return EventTopics.PAYMENT_PROCESSING;
    }
}
