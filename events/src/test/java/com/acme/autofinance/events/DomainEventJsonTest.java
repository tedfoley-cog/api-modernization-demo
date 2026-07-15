package com.acme.autofinance.events;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class DomainEventJsonTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void allEventsRoundTripWithStableMetadata() throws Exception {
        DomainEvent[] events = {
                new LoanApplicationSubmitted(
                        1L, "APP-1", "Alice", amount("25000"), "VIN-1", 2L),
                new CreditDecisionMade(
                        1L, "APPROVED", 740, "A", amount("24000"), amount("5.25")),
                new LoanFunded(
                        1L, "APP-1", LocalDate.of(2026, 7, 15),
                        amount("24000"), amount("5.25"), 60),
                new PaymentReceived(
                        3L, 1L, amount("500"), "ACH", "CONF-1"),
                new PaymentAllocated(
                        3L, 1L, amount("400"), amount("90"), amount("10")),
                new PaymentProcessed(
                        3L, 1L, LocalDate.of(2026, 7, 15),
                        "COMPLETED", amount("400"), amount("23600")),
                new LateFeesAssessed(
                        1L, amount("25"), 15, LocalDate.of(2026, 7, 15)),
                new AccountCreated(
                        4L, "ACC-1", 1L, "Alice", amount("24000")),
                new AccountBalanceUpdated(
                        4L, amount("24000"), amount("23600"), amount("-400"), "PAYMENT"),
                new AccountDelinquent(
                        4L, 1L, 30, "30_DAYS", amount("23600")),
                new DealPackageSubmitted(
                        "DEAL-1", 2L, "VIN-1", amount("26000"), amount("2000"), amount("0")),
                new DealerSettlementCalculated(
                        2L, "DLR-1", amount("500"), amount("100"), amount("400"))
        };

        for (DomainEvent event : events) {
            String json = objectMapper.writeValueAsString(event);
            DomainEvent restored = objectMapper.readValue(json, event.getClass());

            assertEquals(event.getEventId(), restored.getEventId());
            assertEquals(event.getOccurredAt(), restored.getOccurredAt());
            assertEquals(event.getAggregateId(), restored.getAggregateId());
            assertEquals(json, objectMapper.writeValueAsString(restored));
        }
    }

    private static BigDecimal amount(String value) {
        return new BigDecimal(value);
    }
}
