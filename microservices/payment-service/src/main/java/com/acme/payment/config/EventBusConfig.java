package com.acme.payment.config;

import com.acme.payment.domain.event.PaymentEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * In-memory synchronous event bus configuration for the migration demo.
 *
 * Production note: replace with Spring Cloud Stream + Kafka binder for real
 * delivery guarantees, ordering (partition by loanId), and durability via
 * replication factor >= 3.
 */
@Configuration
public class EventBusConfig {

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
            synchronized (events) {
                return Collections.unmodifiableList(new ArrayList<>(events));
            }
        }

        public List<PaymentEvent> getByLoanId(Long loanId) {
            List<PaymentEvent> result = new ArrayList<>();
            synchronized (events) {
                for (PaymentEvent event : events) {
                    if (event.getLoanId().equals(loanId)) {
                        result.add(event);
                    }
                }
            }
            return result;
        }

        public void clear() {
            events.clear();
        }
    }
}
