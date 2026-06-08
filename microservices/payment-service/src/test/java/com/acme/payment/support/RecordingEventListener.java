package com.acme.payment.support;

import com.acme.payment.event.DomainEvent;
import com.acme.payment.event.LateFeesAssessed;
import com.acme.payment.event.PaymentAllocated;
import com.acme.payment.event.PaymentProcessed;
import com.acme.payment.event.PaymentReceived;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Test-only listener that records every published domain event so assertions
 * can verify the command side emits the expected events.
 */
public class RecordingEventListener {

    private final List<DomainEvent> events = new CopyOnWriteArrayList<>();

    @EventListener
    public void onReceived(PaymentReceived e) {
        events.add(e);
    }

    @EventListener
    public void onAllocated(PaymentAllocated e) {
        events.add(e);
    }

    @EventListener
    public void onProcessed(PaymentProcessed e) {
        events.add(e);
    }

    @EventListener
    public void onLateFee(LateFeesAssessed e) {
        events.add(e);
    }

    public void clear() {
        events.clear();
    }

    public List<DomainEvent> all() {
        return events;
    }

    @SuppressWarnings("unchecked")
    public <T extends DomainEvent> List<T> ofType(Class<T> type) {
        List<T> result = new java.util.ArrayList<>();
        for (DomainEvent e : events) {
            if (type.isInstance(e)) {
                result.add((T) e);
            }
        }
        return result;
    }

    @TestConfiguration
    public static class Config {
        @Bean
        public RecordingEventListener recordingEventListener() {
            return new RecordingEventListener();
        }
    }
}
