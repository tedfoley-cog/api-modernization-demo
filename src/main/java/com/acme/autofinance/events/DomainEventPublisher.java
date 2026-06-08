package com.acme.autofinance.events;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Thin wrapper around Spring's {@link ApplicationEventPublisher}.
 *
 * <p>Acts as the single seam through which domain events are emitted. Today it
 * dispatches in-process via Spring Application Events; swapping this
 * implementation for a Kafka/RabbitMQ producer is the only change required to
 * move the Payment context out of the monolith.
 */
@Component
public class DomainEventPublisher {

    private final ApplicationEventPublisher delegate;

    @Autowired
    public DomainEventPublisher(ApplicationEventPublisher delegate) {
        this.delegate = delegate;
    }

    public void publish(DomainEvent event) {
        delegate.publishEvent(event);
    }
}
