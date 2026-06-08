package com.acme.payment.event;

/**
 * Abstraction over the event transport. The command side depends on this rather
 * than on any concrete messaging technology, so the backing implementation can
 * be swapped (in-process today, Kafka/SNS later) without touching domain logic.
 */
public interface EventPublisher {

    void publish(DomainEvent event);
}
