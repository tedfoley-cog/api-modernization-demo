package com.acme.payment.event;

/**
 * Abstraction for publishing domain events. Keeping this behind an interface means the
 * command side depends only on the act of publishing, not on any particular transport
 * (in-process Spring events today, a message broker such as Kafka tomorrow).
 */
public interface EventPublisher {

    void publish(DomainEvent event);
}
