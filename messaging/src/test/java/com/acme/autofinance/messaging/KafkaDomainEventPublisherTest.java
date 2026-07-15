package com.acme.autofinance.messaging;

import static org.mockito.Mockito.verify;

import com.acme.autofinance.events.EventTopics;
import com.acme.autofinance.events.PaymentReceived;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

@ExtendWith(MockitoExtension.class)
class KafkaDomainEventPublisherTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Test
    void delegatesToKafkaTemplateWithTopicAndAggregateKey() {
        KafkaDomainEventPublisher publisher = new KafkaDomainEventPublisher(kafkaTemplate);
        PaymentReceived event = new PaymentReceived(500L, 10L, new BigDecimal("300.00"), "ACH", "CONF-500");

        publisher.publish(event);

        verify(kafkaTemplate).send(EventTopics.PAYMENT_PROCESSING, "500", event);
    }
}
