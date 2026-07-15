package com.acme.autofinance.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Base contract for all domain events. Carries the metadata every event needs:
 * a unique event id, the moment the event occurred, and the id of the aggregate
 * the event is about (used as the Kafka partition key).
 *
 * <p>Immutable and Java-8 compatible (no records). Concrete events declare their
 * own immutable payload fields and expose {@link #eventType()} and {@link #topic()}.
 */
public abstract class DomainEvent {

    private final String eventId;
    private final Instant occurredAt;
    private final String aggregateId;

    protected DomainEvent(String aggregateId) {
        this(UUID.randomUUID().toString(), Instant.now(), aggregateId);
    }

    protected DomainEvent(String eventId, Instant occurredAt, String aggregateId) {
        this.eventId = eventId;
        this.occurredAt = occurredAt;
        this.aggregateId = aggregateId;
    }

    /** Unique identifier for this event instance. */
    public String getEventId() {
        return eventId;
    }

    /** Timestamp at which the business occurrence happened. */
    public Instant getOccurredAt() {
        return occurredAt;
    }

    /** Identifier of the aggregate this event concerns; used as the Kafka key. */
    public String getAggregateId() {
        return aggregateId;
    }

    /** Stable logical name for this event type. */
    public abstract String eventType();

    /** Kafka topic this event is published to. */
    public abstract String topic();
}
