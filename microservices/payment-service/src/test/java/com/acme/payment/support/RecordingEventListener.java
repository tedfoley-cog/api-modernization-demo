package com.acme.payment.support;

import com.acme.payment.event.DomainEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Test-only listener that captures every published {@link DomainEvent} so tests can
 * assert which events the payment service emitted (and in what order).
 */
@Component
public class RecordingEventListener {

    private final List<DomainEvent> events = Collections.synchronizedList(new ArrayList<>());

    @EventListener
    public void on(DomainEvent event) {
        events.add(event);
    }

    public void clear() {
        events.clear();
    }

    public List<DomainEvent> all() {
        synchronized (events) {
            return new ArrayList<>(events);
        }
    }

    public List<String> typesInOrder() {
        return all().stream().map(DomainEvent::getEventType).collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    public <T extends DomainEvent> List<T> ofType(Class<T> type) {
        return all().stream()
                .filter(type::isInstance)
                .map(e -> (T) e)
                .collect(Collectors.toList());
    }
}
