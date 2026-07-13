package com.acme.autofinance.event;

import java.util.Date;
import java.util.UUID;

/**
 * Base class for payment domain events.
 * In production, these would be serialized to Kafka topics with loanId as the partition key.
 * The in-memory ApplicationEventPublisher simulates this for demo portability.
 */
public abstract class PaymentEvent {

    private final String eventId;
    private final String eventType;
    private final Date occurredAt;
    private final Long loanId;

    protected PaymentEvent(String eventType, Long loanId) {
        this.eventId = UUID.randomUUID().toString();
        this.eventType = eventType;
        this.occurredAt = new Date();
        this.loanId = loanId;
    }

    public String getEventId() { return eventId; }
    public String getEventType() { return eventType; }
    public Date getOccurredAt() { return occurredAt; }
    public Long getLoanId() { return loanId; }
}
