package com.acme.payment.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Publishes domain events via Spring's ApplicationEventPublisher.
 * In production, this would publish to Kafka, RabbitMQ, or SNS/SQS.
 * Using Spring events for demo to keep infrastructure simple.
 */
@Component
public class EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(EventPublisher.class);

    private final ApplicationEventPublisher applicationEventPublisher;

    public EventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    public void publish(DomainEvent event) {
        log.info("Publishing domain event: {} [id={}]", event.getEventType(), event.getEventId());
        applicationEventPublisher.publishEvent(event);
    }
}
