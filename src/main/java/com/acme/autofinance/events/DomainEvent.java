package com.acme.autofinance.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Base type for all domain events in the auto-finance platform.
 *
 * <p>Carries the cross-cutting envelope every event shares: a unique
 * {@code eventId} and a {@code timestamp}. Concrete events add the domain
 * payload (loan id, payment id, amount, etc.). Events are immutable.
 */
public abstract class DomainEvent {

    private final String eventId;
    private final Instant timestamp;

    protected DomainEvent() {
        this.eventId = UUID.randomUUID().toString();
        this.timestamp = Instant.now();
    }

    public String getEventId() {
        return eventId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}
