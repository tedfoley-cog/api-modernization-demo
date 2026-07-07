package com.acme.payment.config;

import com.acme.payment.domain.event.PaymentEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * In-memory event bus configuration simulating Kafka.
 *
 * Production note: replace with Spring Cloud Stream + Kafka binder for real
 * delivery guarantees, ordering (partition by loanId), and durability via
 * replication factor >= 3.
 */
@Configuration
public class EventBusConfig {

    private static final Logger log = LoggerFactory.getLogger(EventBusConfig.class);

    private final List<PaymentEvent> publishedEvents = Collections.synchronizedList(new ArrayList<>());

    @Bean
    public EventStore eventStore() {
        return new EventStore(publishedEvents);
    }

    /**
     * Simple event store for audit/debugging — records all published events.
     * In production this would be Kafka's commit log.
     */
    public static class EventStore {
        private final List<PaymentEvent> events;

        public EventStore(List<PaymentEvent> events) {
            this.events = events;
        }

        public void record(PaymentEvent event) {
            events.add(event);
        }

        public List<PaymentEvent> getAll() {
            return Collections.unmodifiableList(events);
        }

        public List<PaymentEvent> getByLoanId(Long loanId) {
            List<PaymentEvent> result = new ArrayList<>();
            for (PaymentEvent event : events) {
                if (event.getLoanId().equals(loanId)) {
                    result.add(event);
                }
            }
            return result;
        }

        public void clear() {
            events.clear();
        }
    }
}
