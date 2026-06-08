package com.acme.payment.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Base type for all immutable domain events emitted by the payment context.
 *
 * <p>Every event carries an identity and a timestamp so downstream consumers can
 * de-duplicate and order them. Subclasses are immutable records of something that has
 * already happened (past tense) — they replace the synchronous cross-domain calls the
 * monolith made inline.
 */
public abstract class DomainEvent {

    private final String eventId = UUID.randomUUID().toString();
    private final Instant occurredAt = Instant.now();

    public String getEventId() {
        return eventId;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    /** Stable, human-readable event type used for routing/serialization. */
    public abstract String getEventType();
}
