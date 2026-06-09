package com.acme.payment.event;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * {@link EventPublisher} backed by Spring's {@link ApplicationEventPublisher}.
 *
 * <p>Publishing in-process keeps the demo self-contained while still modelling the
 * async, fire-and-forget contract: the command side hands off an event and does not
 * wait on (or know about) any downstream domain.
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
