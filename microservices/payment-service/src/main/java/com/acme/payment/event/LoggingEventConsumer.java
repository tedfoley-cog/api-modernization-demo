package com.acme.payment.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * In-process consumer that records every published {@link DomainEvent} by logging it.
 *
 * <p>In a real system the loan and account domains would subscribe to these events
 * (e.g. via a Kafka topic) to update their own state. Here a logging listener stands in
 * for those subscribers, demonstrating that the payment service merely <em>publishes</em>
 * facts and never calls into other domains directly.
 */
@Component
public class LoggingEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(LoggingEventConsumer.class);

    @EventListener
    public void on(DomainEvent event) {
        log.info("Domain event published: type={} eventId={} aggregateId={} occurredAt={}",
                event.getEventType(), event.getEventId(), event.getAggregateId(), event.getOccurredAt());
    }
}
