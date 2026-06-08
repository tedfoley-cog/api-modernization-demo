package com.acme.payment.event;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * {@link EventPublisher} backed by Spring's {@link ApplicationEventPublisher}.
 * In-process today; the interface lets us replace this with a broker-backed
 * implementation without changing callers.
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
