package com.acme.payment.event;

/**
 * Abstraction over the event bus. In production this would be backed by Kafka, SNS/SQS
 * or RabbitMQ; here it is backed by Spring's in-process {@code ApplicationEventPublisher}
 * so the decoupling pattern is demonstrable without external infrastructure.
 */
public interface EventPublisher {

    void publish(DomainEvent event);
}
