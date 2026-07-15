package com.acme.autofinance.events;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class DomainEventMetadataTest {

    @Test
    void populatesEventMetadataFromAggregateId() {
        LoanApplicationSubmitted event = new LoanApplicationSubmitted(
                42L, "APP-42", "Jane Doe", new BigDecimal("25000.00"), "1FTFW1E50NF000001", 7L);

        assertNotNull(event.getEventId());
        assertNotNull(event.getOccurredAt());
        assertEquals("42", event.getAggregateId());
        assertEquals("LoanApplicationSubmitted", event.eventType());
    }

    @Test
    void generatesUniqueEventIdsPerInstance() {
        PaymentReceived first = new PaymentReceived(1L, 10L, BigDecimal.TEN, "ACH", "CONF-1");
        PaymentReceived second = new PaymentReceived(1L, 10L, BigDecimal.TEN, "ACH", "CONF-1");

        assertNotEquals(first.getEventId(), second.getEventId());
    }

    @Test
    void usesStringAggregateIdForNonNumericKeys() {
        DealPackageSubmitted event = new DealPackageSubmitted(
                "DEAL-9001", 3L, "1FTFW1E50NF000002",
                new BigDecimal("40000"), new BigDecimal("5000"), new BigDecimal("2000"));

        assertEquals("DEAL-9001", event.getAggregateId());
    }
}
