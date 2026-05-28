package com.acme.payment;

import com.acme.payment.command.AssessLateFeeCommand;
import com.acme.payment.command.PaymentCommandHandler;
import com.acme.payment.command.SubmitPaymentCommand;
import com.acme.payment.event.LateFeesAssessed;
import com.acme.payment.event.PaymentAllocated;
import com.acme.payment.event.PaymentProcessed;
import com.acme.payment.event.PaymentReceived;
import com.acme.payment.model.Payment;
import com.acme.payment.model.PaymentStatus;
import com.acme.payment.query.PaymentQueryHandler;
import com.acme.payment.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for the Payment Processing microservice.
 * Verifies CQRS command/query separation, domain event publishing,
 * and end-to-end payment lifecycle.
 */
@SpringBootTest
@ActiveProfiles("test")
public class PaymentServiceIntegrationTest {

    @Autowired
    private PaymentCommandHandler commandHandler;

    @Autowired
    private PaymentQueryHandler queryHandler;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private TestEventCollector eventCollector;

    @BeforeEach
    void setUp() {
        eventCollector.clear();
    }

    @Test
    void submitPayment_publishesPaymentReceivedEvent() {
        SubmitPaymentCommand cmd = new SubmitPaymentCommand(
                100L, new BigDecimal("525.89"), "ACH", "021000021", "****1234");

        Payment result = commandHandler.handle(cmd);

        assertNotNull(result.getId());
        assertNotNull(result.getConfirmationNumber());
        assertTrue(result.getConfirmationNumber().startsWith("PMT-"));
        assertEquals(100L, result.getLoanId().longValue());
        assertEquals(new BigDecimal("525.89"), result.getPaymentAmount());

        assertTrue(eventCollector.hasEvent(PaymentReceived.class),
                "PaymentReceived event should be published");
    }

    @Test
    void submitPayment_allocatesPaymentCorrectly() {
        SubmitPaymentCommand cmd = new SubmitPaymentCommand(
                101L, new BigDecimal("500.00"), "CHECK", null, null);

        Payment result = commandHandler.handle(cmd);

        assertNotNull(result.getPrincipalAmount());
        assertNotNull(result.getInterestAmount());
        assertNotNull(result.getFeeAmount());

        BigDecimal total = result.getPrincipalAmount()
                .add(result.getInterestAmount())
                .add(result.getFeeAmount());
        assertEquals(0, new BigDecimal("500.00").compareTo(total),
                "Allocated amounts should sum to payment amount");

        assertTrue(eventCollector.hasEvent(PaymentAllocated.class),
                "PaymentAllocated event should be published");
    }

    @Test
    void submitAchPayment_processesAndPublishesPaymentProcessed() {
        SubmitPaymentCommand cmd = new SubmitPaymentCommand(
                102L, new BigDecimal("300.00"), "ACH", "021000021", "****5678");

        Payment result = commandHandler.handle(cmd);

        assertEquals(PaymentStatus.COMPLETED, result.getStatus());
        assertNotNull(result.getProcessedDate());

        assertTrue(eventCollector.hasEvent(PaymentProcessed.class),
                "PaymentProcessed event should be published for ACH payments");
    }

    @Test
    void submitAchPayment_failsWithInvalidRoutingNumber() {
        SubmitPaymentCommand cmd = new SubmitPaymentCommand(
                103L, new BigDecimal("250.00"), "ACH", "12345", "****9999");

        Payment result = commandHandler.handle(cmd);

        assertEquals(PaymentStatus.FAILED, result.getStatus(),
                "Payment with invalid routing number should fail");
    }

    @Test
    void submitCheckPayment_remainsPending() {
        SubmitPaymentCommand cmd = new SubmitPaymentCommand(
                104L, new BigDecimal("400.00"), "CHECK", null, null);

        Payment result = commandHandler.handle(cmd);

        assertEquals(PaymentStatus.PENDING, result.getStatus(),
                "Check payments should stay PENDING (not auto-processed)");
    }

    @Test
    void assessLateFee_publishesLateFeesAssessedEvent() {
        AssessLateFeeCommand cmd = new AssessLateFeeCommand(
                200L, 25, new BigDecimal("10000.00"));

        Payment result = commandHandler.handle(cmd);

        assertNotNull(result.getId());
        assertEquals(new BigDecimal("25.00"), result.getLateFee());
        assertEquals(BigDecimal.ZERO, result.getPaymentAmount());
        assertTrue(result.getConfirmationNumber().startsWith("FEE-"));

        assertTrue(eventCollector.hasEvent(LateFeesAssessed.class),
                "LateFeesAssessed event should be published");
    }

    @Test
    void assessLateFee_usesPercentageForOver30Days() {
        AssessLateFeeCommand cmd = new AssessLateFeeCommand(
                201L, 45, new BigDecimal("10000.00"));

        Payment result = commandHandler.handle(cmd);

        assertEquals(new BigDecimal("50.00"), result.getLateFee(),
                "Late fee should be capped at $50 for high balances");
    }

    @Test
    void queryPaymentHistory_returnsSeededPayments() {
        List<Payment> history = queryHandler.getPaymentHistory(1L);

        assertFalse(history.isEmpty(), "Should have seeded payments for loan 1");
        assertTrue(history.size() >= 2, "Loan 1 should have at least 2 payments");
    }

    @Test
    void queryPendingPayments_returnsPendingItems() {
        List<Payment> pending = queryHandler.getPendingPayments();

        assertNotNull(pending);
        for (Payment p : pending) {
            assertEquals(PaymentStatus.PENDING, p.getStatus());
        }
    }

    @Test
    void queryTotalPaid_calculatesCorrectSum() {
        BigDecimal total = queryHandler.getTotalPaidForLoan(1L);

        assertNotNull(total);
        assertTrue(total.compareTo(BigDecimal.ZERO) > 0,
                "Total paid for loan 1 should be positive");
    }

    @Test
    void cqrsCommandQuerySeparation_writeThenRead() {
        SubmitPaymentCommand cmd = new SubmitPaymentCommand(
                300L, new BigDecimal("750.00"), "WIRE", null, null);

        Payment written = commandHandler.handle(cmd);

        Payment read = queryHandler.getPaymentById(written.getId());

        assertEquals(written.getId(), read.getId());
        assertEquals(written.getConfirmationNumber(), read.getConfirmationNumber());
        assertEquals(written.getPaymentAmount(), read.getPaymentAmount());
    }

    @Test
    void multipleEventsPublishedForAchPayment() {
        SubmitPaymentCommand cmd = new SubmitPaymentCommand(
                400L, new BigDecimal("1000.00"), "ACH", "021000021", "****0001");

        commandHandler.handle(cmd);

        assertTrue(eventCollector.hasEvent(PaymentReceived.class));
        assertTrue(eventCollector.hasEvent(PaymentAllocated.class));
        assertTrue(eventCollector.hasEvent(PaymentProcessed.class));

        assertTrue(eventCollector.getEventCount() >= 3,
                "ACH payment should publish at least 3 events");
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        public TestEventCollector testEventCollector() {
            return new TestEventCollector();
        }
    }

    static class TestEventCollector {

        private final List<Object> events = new CopyOnWriteArrayList<>();

        @EventListener
        public void onEvent(PaymentReceived event) { events.add(event); }

        @EventListener
        public void onEvent(PaymentAllocated event) { events.add(event); }

        @EventListener
        public void onEvent(PaymentProcessed event) { events.add(event); }

        @EventListener
        public void onEvent(LateFeesAssessed event) { events.add(event); }

        public boolean hasEvent(Class<?> eventType) {
            return events.stream().anyMatch(eventType::isInstance);
        }

        public int getEventCount() { return events.size(); }

        public void clear() { events.clear(); }
    }
}
