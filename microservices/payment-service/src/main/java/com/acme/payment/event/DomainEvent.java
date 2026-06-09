package com.acme.payment.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Common immutable base for all payment domain events.
 *
 * <p>Each event carries a unique {@code eventId}, the {@code occurredAt} timestamp and
 * the {@code aggregateId} it pertains to. Subclasses add their own immutable payload and
 * never expose mutators, so a published event can be safely shared with any number of
 * downstream consumers.
 */
public abstract class DomainEvent {

    private final String eventId;
    private final Instant occurredAt;
    private final String aggregateId;

    protected DomainEvent(String aggregateId) {
        this.eventId = UUID.randomUUID().toString();
        this.occurredAt = Instant.now();
        this.aggregateId = aggregateId;
    }

    public String getEventId() {
        return eventId;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public String getAggregateId() {
        return aggregateId;
    }

    /** Stable logical name of the event type, used in logs and schema mapping. */
    public abstract String getEventType();
}
