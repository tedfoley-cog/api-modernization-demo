package com.acme.payment.event;

import java.util.Date;
import java.util.UUID;

/**
 * Base class for all domain events in the payment bounded context.
 */
public abstract class DomainEvent {

    private final String eventId;
    private final String eventType;
    private final Date occurredAt;

    protected DomainEvent(String eventType) {
        this.eventId = UUID.randomUUID().toString();
        this.eventType = eventType;
        this.occurredAt = new Date();
    }

    public String getEventId() { return eventId; }
    public String getEventType() { return eventType; }
    public Date getOccurredAt() { return occurredAt; }
}
