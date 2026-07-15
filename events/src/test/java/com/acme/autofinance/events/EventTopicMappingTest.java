package com.acme.autofinance.events;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class EventTopicMappingTest {

    @Test
    void loanOriginationEventsMapToLoanTopic() {
        List<DomainEvent> events = Arrays.asList(
                new LoanApplicationSubmitted(1L, "APP-1", "A", BigDecimal.ONE, "VIN", 1L),
                new CreditDecisionMade(1L, "APPROVED", 720, "A", BigDecimal.ONE, BigDecimal.ONE),
                new LoanFunded(1L, "APP-1", LocalDate.now(), BigDecimal.ONE, BigDecimal.ONE, 60));
        events.forEach(e -> assertEquals(EventTopics.LOAN_ORIGINATION, e.topic(), e.eventType()));
    }

    @Test
    void paymentEventsMapToPaymentTopic() {
        List<DomainEvent> events = Arrays.asList(
                new PaymentReceived(1L, 1L, BigDecimal.ONE, "ACH", "C-1"),
                new PaymentAllocated(1L, 1L, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE),
                new PaymentProcessed(1L, 1L, LocalDate.now(), "COMPLETED", BigDecimal.ONE, BigDecimal.ONE),
                new LateFeesAssessed(1L, BigDecimal.ONE, 30, LocalDate.now()));
        events.forEach(e -> assertEquals(EventTopics.PAYMENT_PROCESSING, e.topic(), e.eventType()));
    }

    @Test
    void accountEventsMapToAccountTopic() {
        List<DomainEvent> events = Arrays.asList(
                new AccountCreated(1L, "ACC-1", 1L, "Cust", BigDecimal.ONE),
                new AccountBalanceUpdated(1L, BigDecimal.TEN, BigDecimal.ONE, BigDecimal.ONE, "PAYMENT"),
                new AccountDelinquent(1L, 1L, 45, "30-59", BigDecimal.TEN));
        events.forEach(e -> assertEquals(EventTopics.ACCOUNT_SERVICING, e.topic(), e.eventType()));
    }

    @Test
    void dealerEventsMapToDealerTopic() {
        List<DomainEvent> events = Arrays.asList(
                new DealPackageSubmitted("DEAL-1", 1L, "VIN", BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE),
                new DealerSettlementCalculated(1L, "D-1", BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE));
        events.forEach(e -> assertEquals(EventTopics.DEALER_INTEGRATION, e.topic(), e.eventType()));
    }

    @Test
    void topicNamesAreStable() {
        assertEquals("autofinance.loan-origination.events", EventTopics.LOAN_ORIGINATION);
        assertEquals("autofinance.payment-processing.events", EventTopics.PAYMENT_PROCESSING);
        assertEquals("autofinance.account-servicing.events", EventTopics.ACCOUNT_SERVICING);
        assertEquals("autofinance.dealer-integration.events", EventTopics.DEALER_INTEGRATION);
    }
}
