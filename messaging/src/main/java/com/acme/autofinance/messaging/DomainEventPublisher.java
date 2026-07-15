package com.acme.autofinance.messaging;

import com.acme.autofinance.events.DomainEvent;

/**
 * Abstraction for publishing domain events to the messaging backbone. Services
 * depend on this interface rather than on a concrete Kafka client, keeping them
 * decoupled from the transport and from one another.
 */
public interface DomainEventPublisher {

    /**
     * Publish a domain event. Implementations route to the event's own
     * {@link DomainEvent#topic()} keyed by its {@link DomainEvent#getAggregateId()}.
     */
    void publish(DomainEvent event);
}
