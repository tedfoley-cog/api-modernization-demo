package com.acme.payment.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Base type for all domain events. Carries the cross-cutting envelope metadata
 * (id, timestamp, type, schema version). Subclasses are immutable and hold only
 * business payload fields.
 */
public abstract class DomainEvent {

    private final UUID eventId;
    private final Instant occurredAt;
    private final String eventType;
    private final int version;

    protected DomainEvent(String eventType, int version) {
        this.eventId = UUID.randomUUID();
        this.occurredAt = Instant.now();
        this.eventType = eventType;
        this.version = version;
    }

    public UUID getEventId() {
        return eventId;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public String getEventType() {
        return eventType;
    }

    public int getVersion() {
        return version;
    }
}
