package com.acme.payment.event;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Default {@link EventPublisher} backed by Spring's application event multicaster.
 *
 * <p>Swapping this for a Kafka/SNS implementation is the only change required to move
 * from in-process to cross-service eventing — the domain code only depends on the
 * {@link EventPublisher} interface.
 */
@Component
public class SpringEventPublisher implements EventPublisher {

    private final ApplicationEventPublisher delegate;

    public SpringEventPublisher(ApplicationEventPublisher delegate) {
        this.delegate = delegate;
    }

    @Override
    public void publish(DomainEvent event) {
        delegate.publishEvent(event);
    }
}
