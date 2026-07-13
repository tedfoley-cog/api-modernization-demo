package com.acme.payment;

import com.acme.payment.config.EventBusConfig;
import com.acme.payment.domain.event.LateFeeAssessed;
import com.acme.payment.domain.event.PaymentCompleted;
import com.acme.payment.domain.event.PaymentEvent;
import com.acme.payment.domain.event.PaymentFailed;
import com.acme.payment.domain.event.PaymentReceived;
import com.acme.payment.domain.model.Payment;
import com.acme.payment.domain.model.PaymentMethod;
import com.acme.payment.domain.model.PaymentStatus;
import com.acme.payment.domain.model.PaymentSummary;
import com.acme.payment.domain.repository.PaymentRepository;
import com.acme.payment.domain.repository.PaymentSummaryRepository;
import com.acme.payment.service.LateFeeService;
import com.acme.payment.service.PaymentCalculationService;
import com.acme.payment.service.PaymentCommandService;
import com.acme.payment.service.PaymentQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class PaymentServiceIntegrationTest {

    @Autowired
    private PaymentCommandService commandService;

    @Autowired
    private PaymentQueryService queryService;

    @Autowired
    private PaymentCalculationService calculationService;

    @Autowired
    private LateFeeService lateFeeService;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PaymentSummaryRepository summaryRepository;

    @Autowired
    private EventBusConfig.EventStore eventStore;

    @BeforeEach
    void setUp() {
        eventStore.clear();
    }

    // ========================================================================
    // Payment Calculation Tests
    // ========================================================================

    @Test
    void calculateMonthlyPayment_standardAmortization() {
        BigDecimal principal = new BigDecimal("28500.00");
        BigDecimal rate = new BigDecimal("3.99");
        int term = 60;

        BigDecimal monthly = calculationService.calculateMonthlyPayment(principal, rate, term);

        assertNotNull(monthly);
        assertTrue(monthly.compareTo(BigDecimal.ZERO) > 0);
        // Amortization formula: M = P * [r(1+r)^n] / [(1+r)^n - 1]
        // For 28500 @ 3.99% / 60mo, the exact result is 524.74
        assertEquals(new BigDecimal("524.74"), monthly);
    }

    @Test
    void calculateMonthlyPayment_zeroRate() {
        BigDecimal principal = new BigDecimal("12000.00");
        BigDecimal rate = BigDecimal.ZERO;
        int term = 60;

        BigDecimal monthly = calculationService.calculateMonthlyPayment(principal, rate, term);

        assertEquals(new BigDecimal("200.00"), monthly);
    }

    @Test
    void calculateMonthlyPayment_nullInputs() {
        assertEquals(BigDecimal.ZERO, calculationService.calculateMonthlyPayment(null, null, 60));
        assertEquals(BigDecimal.ZERO, calculationService.calculateMonthlyPayment(BigDecimal.TEN, null, 60));
        assertEquals(BigDecimal.ZERO, calculationService.calculateMonthlyPayment(null, BigDecimal.TEN, 60));
        assertEquals(BigDecimal.ZERO, calculationService.calculateMonthlyPayment(BigDecimal.TEN, BigDecimal.TEN, 0));
    }

    @Test
    void calculatePayoffAmount() {
        BigDecimal payoff = calculationService.calculatePayoffAmount(
                new BigDecimal("28500.00"), new BigDecimal("3.99"), 60);

        assertNotNull(payoff);
        // payoff = monthly * term = 524.74 * 60
        BigDecimal expected = new BigDecimal("524.74").multiply(new BigDecimal("60"))
                .setScale(2, RoundingMode.HALF_UP);
        assertEquals(expected, payoff);
    }

    // ========================================================================
    // Payment Submission + Event Publishing Tests
    // ========================================================================

    @Test
    void submitPayment_publishesPaymentReceivedEvent() {
        Payment payment = new Payment();
        payment.setLoanId(999L);
        payment.setPaymentAmount(new BigDecimal("500.00"));
        payment.setPaymentMethod(PaymentMethod.CHECK);

        Payment result = commandService.submitPayment(payment, new BigDecimal("20000.00"), new BigDecimal("5.49"));

        assertNotNull(result.getId());
        assertNotNull(result.getConfirmationNumber());
        assertTrue(result.getConfirmationNumber().startsWith("PMT-"));
        assertEquals(PaymentStatus.PENDING, result.getStatus());
        assertNotNull(result.getPrincipalAmount());
        assertNotNull(result.getInterestAmount());
        assertNotNull(result.getFeeAmount());

        // Verify event published
        List<PaymentEvent> events = eventStore.getByLoanId(999L);
        assertFalse(events.isEmpty());
        assertTrue(events.get(0) instanceof PaymentReceived);

        PaymentReceived received = (PaymentReceived) events.get(0);
        assertEquals(999L, received.getLoanId());
        assertEquals(result.getId(), received.getPaymentId());
        assertEquals(new BigDecimal("500.00"), received.getTotalAmount());
    }

    @Test
    void submitAchPayment_publishesPaymentCompletedEvent() {
        Payment payment = new Payment();
        payment.setLoanId(998L);
        payment.setPaymentAmount(new BigDecimal("300.00"));
        payment.setPaymentMethod(PaymentMethod.ACH);
        payment.setAchRoutingNumber("021000021");
        payment.setAchAccountNumber("****1234");

        Payment result = commandService.submitPayment(payment, new BigDecimal("15000.00"), new BigDecimal("5.49"));

        assertEquals(PaymentStatus.COMPLETED, result.getStatus());
        assertNotNull(result.getProcessedDate());

        // Verify both PaymentReceived and PaymentCompleted events
        List<PaymentEvent> events = eventStore.getByLoanId(998L);
        assertEquals(2, events.size());
        assertTrue(events.get(0) instanceof PaymentReceived);
        assertTrue(events.get(1) instanceof PaymentCompleted);

        PaymentCompleted completed = (PaymentCompleted) events.get(1);
        assertEquals(998L, completed.getLoanId());
        assertNotNull(completed.getTotalPaidToDate());
    }

    @Test
    void submitAchPayment_invalidRouting_publishesPaymentFailedEvent() {
        Payment payment = new Payment();
        payment.setLoanId(997L);
        payment.setPaymentAmount(new BigDecimal("200.00"));
        payment.setPaymentMethod(PaymentMethod.ACH);
        payment.setAchRoutingNumber("123"); // Invalid: not 9 digits

        Payment result = commandService.submitPayment(payment, new BigDecimal("10000.00"), new BigDecimal("5.49"));

        assertEquals(PaymentStatus.FAILED, result.getStatus());

        List<PaymentEvent> events = eventStore.getByLoanId(997L);
        assertEquals(2, events.size());
        assertTrue(events.get(0) instanceof PaymentReceived);
        assertTrue(events.get(1) instanceof PaymentFailed);

        PaymentFailed failed = (PaymentFailed) events.get(1);
        assertEquals("Invalid ACH routing number", failed.getReason());
    }

    // ========================================================================
    // Late Fee Tests
    // ========================================================================

    @Test
    void assessLateFee_flatFee_under30days() {
        Payment fee = lateFeeService.assessLateFee(996L, 20, new BigDecimal("10000.00"));

        assertNotNull(fee.getId());
        assertEquals(new BigDecimal("25.00"), fee.getLateFee());
        assertEquals(PaymentStatus.PENDING, fee.getStatus());
        assertTrue(fee.getConfirmationNumber().startsWith("FEE-"));

        List<PaymentEvent> events = eventStore.getByLoanId(996L);
        assertEquals(1, events.size());
        assertTrue(events.get(0) instanceof LateFeeAssessed);
        assertEquals(new BigDecimal("25.00"), ((LateFeeAssessed) events.get(0)).getFeeAmount());
    }

    @Test
    void assessLateFee_percentageFee_over30days() {
        Payment fee = lateFeeService.assessLateFee(995L, 45, new BigDecimal("10000.00"));

        // 5% of 10000 = 500, capped at MAX_LATE_FEE = 50
        assertEquals(new BigDecimal("50.00"), fee.getLateFee());

        List<PaymentEvent> events = eventStore.getByLoanId(995L);
        assertTrue(events.get(0) instanceof LateFeeAssessed);
    }

    @Test
    void assessLateFee_percentageFee_smallBalance() {
        Payment fee = lateFeeService.assessLateFee(994L, 45, new BigDecimal("500.00"));

        // 5% of 500 = 25, under MAX_LATE_FEE cap
        assertEquals(new BigDecimal("25.00"), fee.getLateFee());
    }

    // ========================================================================
    // CQRS Read Model Tests
    // ========================================================================

    @Test
    void paymentSummary_seededData() {
        // Verify seed data from data.sql
        Optional<PaymentSummary> summary = queryService.getPaymentSummary(1L);
        assertTrue(summary.isPresent());
        assertEquals(new BigDecimal("1051.78"), summary.get().getTotalPaid());
        assertEquals(2, summary.get().getPaymentCount());
    }

    @Test
    void paymentMetrics_aggregatesAllSummaries() {
        Map<String, Object> metrics = queryService.getPaymentMetrics();

        assertNotNull(metrics.get("totalCollected"));
        assertNotNull(metrics.get("totalPrincipalPaid"));
        assertNotNull(metrics.get("totalInterestPaid"));
        assertNotNull(metrics.get("totalPaymentCount"));
        assertNotNull(metrics.get("activeLoanCount"));

        assertTrue(((BigDecimal) metrics.get("totalCollected")).compareTo(BigDecimal.ZERO) > 0);
        assertTrue((int) metrics.get("totalPaymentCount") > 0);
    }

    @Test
    void paymentStatusBreakdown() {
        Map<String, Long> breakdown = queryService.getPaymentStatusBreakdown();

        assertNotNull(breakdown);
        assertTrue(breakdown.containsKey("COMPLETED"));
        assertTrue(breakdown.get("COMPLETED") > 0);
    }

    // ========================================================================
    // Payment History + Query Tests
    // ========================================================================

    @Test
    void getPaymentHistory_returnsSeedData() {
        List<Payment> history = queryService.getPaymentHistory(1L);

        assertFalse(history.isEmpty());
        assertEquals(2, history.size());
        assertEquals(new BigDecimal("525.89"), history.get(0).getPaymentAmount());
    }

    @Test
    void getPendingPayments() {
        List<Payment> pending = queryService.getPendingPayments();

        assertNotNull(pending);
        // Seed data has 1 pending late fee for loan 8
        assertTrue(pending.size() >= 1);
    }

    // ========================================================================
    // Batch Processing Tests
    // ========================================================================

    @Test
    void processBatchPayments() {
        Payment p1 = new Payment();
        p1.setLoanId(993L);
        p1.setPaymentAmount(new BigDecimal("100.00"));
        p1.setPaymentMethod(PaymentMethod.CHECK);

        Payment p2 = new Payment();
        p2.setLoanId(992L);
        p2.setPaymentAmount(new BigDecimal("200.00"));
        p2.setPaymentMethod(PaymentMethod.CHECK);

        Map<String, Object> results = commandService.processBatchPayments(
                Arrays.asList(p1, p2), new BigDecimal("5000.00"), new BigDecimal("5.49"));

        assertEquals(2, results.get("totalSubmitted"));
        assertEquals(2, results.get("processed"));
        assertEquals(0, results.get("failed"));
    }

    @Test
    void processBatchPayments_isolatesFailedPaymentTransaction() {
        Payment invalid = new Payment();
        invalid.setPaymentAmount(new BigDecimal("100.00"));
        invalid.setPaymentMethod(PaymentMethod.CHECK);

        Payment valid = new Payment();
        valid.setLoanId(990L);
        valid.setPaymentAmount(new BigDecimal("200.00"));
        valid.setPaymentMethod(PaymentMethod.CHECK);

        Map<String, Object> results = commandService.processBatchPayments(
                Arrays.asList(invalid, valid), new BigDecimal("5000.00"), new BigDecimal("5.49"));

        assertEquals(2, results.get("totalSubmitted"));
        assertEquals(1, results.get("processed"));
        assertEquals(1, results.get("failed"));
        assertEquals(1, paymentRepository.findByLoanId(990L).size());
    }

    // ========================================================================
    // Event Store Tests
    // ========================================================================

    @Test
    void eventStore_tracksAllEvents() {
        // Submit a payment to generate events
        Payment payment = new Payment();
        payment.setLoanId(991L);
        payment.setPaymentAmount(new BigDecimal("100.00"));
        payment.setPaymentMethod(PaymentMethod.CHECK);
        commandService.submitPayment(payment, BigDecimal.ZERO, BigDecimal.ZERO);

        // Assess a late fee to generate more events
        lateFeeService.assessLateFee(991L, 20, new BigDecimal("5000.00"));

        List<PaymentEvent> allEvents = eventStore.getAll();
        assertTrue(allEvents.size() >= 2);

        List<PaymentEvent> loanEvents = eventStore.getByLoanId(991L);
        assertEquals(2, loanEvents.size());
        assertTrue(loanEvents.get(0) instanceof PaymentReceived);
        assertTrue(loanEvents.get(1) instanceof LateFeeAssessed);
    }
}
