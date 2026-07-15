package com.acme.autofinance.messaging;

import com.acme.autofinance.events.DomainEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Kafka-backed {@link DomainEventPublisher}. Delegates to a {@link KafkaTemplate},
 * publishing each event to its declared topic using the aggregate id as the
 * partition key so events for the same aggregate keep their order.
 */
@Component
public class KafkaDomainEventPublisher implements DomainEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public KafkaDomainEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void publish(DomainEvent event) {
        kafkaTemplate.send(event.topic(), event.getAggregateId(), event);
    }
}
