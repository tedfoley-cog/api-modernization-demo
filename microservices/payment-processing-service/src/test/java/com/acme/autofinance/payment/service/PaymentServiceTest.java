package com.acme.autofinance.payment.service;

import com.acme.autofinance.events.DomainEvent;
import com.acme.autofinance.events.LateFeesAssessed;
import com.acme.autofinance.events.PaymentAllocated;
import com.acme.autofinance.events.PaymentProcessed;
import com.acme.autofinance.events.PaymentReceived;
import com.acme.autofinance.messaging.DomainEventPublisher;
import com.acme.autofinance.payment.domain.AccountProjection;
import com.acme.autofinance.payment.domain.LoanProjection;
import com.acme.autofinance.payment.domain.Payment;
import com.acme.autofinance.payment.domain.PaymentMethod;
import com.acme.autofinance.payment.domain.PaymentStatus;
import com.acme.autofinance.payment.repository.AccountProjectionRepository;
import com.acme.autofinance.payment.repository.LoanProjectionRepository;
import com.acme.autofinance.payment.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private LoanProjectionRepository loanProjectionRepository;
    @Mock private AccountProjectionRepository accountProjectionRepository;
    @Mock private DomainEventPublisher eventPublisher;
    @Captor private ArgumentCaptor<DomainEvent> eventCaptor;

    private PaymentService service;

    @BeforeEach
    void setUp() {
        service = new PaymentService(paymentRepository, loanProjectionRepository,
                accountProjectionRepository, eventPublisher);
    }

    private void assignIdsOnSave() {
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment p = invocation.getArgument(0);
            if (p.getId() == null) {
                p.setId(1L);
            }
            return p;
        });
    }

    private LoanProjection loan(Long id, String rate) {
        return new LoanProjection(id, "APP-" + id, new BigDecimal("20000.00"),
                rate == null ? null : new BigDecimal(rate), 60);
    }

    private static void assertAmount(String expected, BigDecimal actual) {
        assertTrue(actual != null && new BigDecimal(expected).compareTo(actual) == 0,
                "expected " + expected + " but was " + actual);
    }

    @Test
    void submitAchPaymentAllocatesAndPublishesCatalogEvents() {
        assignIdsOnSave();
        when(loanProjectionRepository.findById(10L)).thenReturn(Optional.of(loan(10L, "6.00")));
        when(accountProjectionRepository.findByLoanId(10L))
                .thenReturn(Optional.of(new AccountProjection(99L, 10L, new BigDecimal("10000.00"))));
        when(paymentRepository.findByLoanIdAndStatus(10L, PaymentStatus.PENDING))
                .thenReturn(Collections.emptyList());

        Payment request = new Payment();
        request.setLoanId(10L);
        request.setPaymentAmount(new BigDecimal("450.00"));
        request.setPaymentMethod(PaymentMethod.ACH);
        request.setAchRoutingNumber("123456789");

        Payment result = service.submitPayment(request);

        assertEquals(PaymentStatus.COMPLETED, result.getStatus());
        // 6% annual -> 0.5% monthly on 10000 balance = 50.00 interest; fees 0; principal 400.
        assertAmount("0.00", result.getFeeAmount());
        assertAmount("50.00", result.getInterestAmount());
        assertAmount("400.00", result.getPrincipalAmount());

        verify(eventPublisher, times(3)).publish(eventCaptor.capture());
        List<DomainEvent> events = eventCaptor.getAllValues();

        PaymentReceived received = (PaymentReceived) events.get(0);
        assertEquals(1L, received.getPaymentId());
        assertEquals(10L, received.getLoanId());
        assertAmount("450.00", received.getPaymentAmount());
        assertEquals("ACH", received.getPaymentMethod());
        assertEquals(result.getConfirmationNumber(), received.getConfirmationNumber());

        PaymentAllocated allocated = (PaymentAllocated) events.get(1);
        assertEquals(10L, allocated.getLoanId());
        assertAmount("400.00", allocated.getPrincipalAmount());
        assertAmount("50.00", allocated.getInterestAmount());
        assertAmount("0.00", allocated.getFeeAmount());

        PaymentProcessed processed = (PaymentProcessed) events.get(2);
        assertEquals("COMPLETED", processed.getStatus());
        assertAmount("400.00", processed.getPrincipalApplied());
        assertAmount("9600.00", processed.getNewBalance());
    }

    @Test
    void nonAchPaymentDoesNotPublishProcessedEvent() {
        assignIdsOnSave();
        when(loanProjectionRepository.findById(10L)).thenReturn(Optional.of(loan(10L, "6.00")));
        when(accountProjectionRepository.findByLoanId(10L))
                .thenReturn(Optional.of(new AccountProjection(99L, 10L, new BigDecimal("10000.00"))));
        when(paymentRepository.findByLoanIdAndStatus(10L, PaymentStatus.PENDING))
                .thenReturn(Collections.emptyList());

        Payment request = new Payment();
        request.setLoanId(10L);
        request.setPaymentAmount(new BigDecimal("450.00"));
        request.setPaymentMethod(PaymentMethod.CHECK);

        Payment result = service.submitPayment(request);

        assertEquals(PaymentStatus.PENDING, result.getStatus());
        // Only PaymentReceived + PaymentAllocated.
        verify(eventPublisher, times(2)).publish(any(DomainEvent.class));
    }

    @Test
    void achPaymentWithInvalidRoutingFailsWithoutProcessedEvent() {
        assignIdsOnSave();
        when(loanProjectionRepository.findById(10L)).thenReturn(Optional.of(loan(10L, "6.00")));
        when(accountProjectionRepository.findByLoanId(10L))
                .thenReturn(Optional.of(new AccountProjection(99L, 10L, new BigDecimal("10000.00"))));
        when(paymentRepository.findByLoanIdAndStatus(10L, PaymentStatus.PENDING))
                .thenReturn(Collections.emptyList());

        Payment request = new Payment();
        request.setLoanId(10L);
        request.setPaymentAmount(new BigDecimal("450.00"));
        request.setPaymentMethod(PaymentMethod.ACH);
        request.setAchRoutingNumber("123");

        Payment result = service.submitPayment(request);

        assertEquals(PaymentStatus.FAILED, result.getStatus());
        verify(eventPublisher, times(2)).publish(any(DomainEvent.class));
    }

    @Test
    void batchProcessingIsDeterministicAndCountsFailures() {
        assignIdsOnSave();
        lenient().when(accountProjectionRepository.findByLoanId(anyLong())).thenReturn(Optional.empty());
        lenient().when(paymentRepository.findByLoanIdAndStatus(anyLong(), any())).thenReturn(Collections.emptyList());
        when(loanProjectionRepository.findById(10L)).thenReturn(Optional.of(loan(10L, "6.00")));
        when(loanProjectionRepository.findById(11L)).thenReturn(Optional.empty());

        Payment good = new Payment();
        good.setLoanId(10L);
        good.setPaymentAmount(new BigDecimal("450.00"));
        good.setPaymentMethod(PaymentMethod.CHECK);

        Payment bad = new Payment();
        bad.setLoanId(11L);
        bad.setPaymentAmount(new BigDecimal("450.00"));
        bad.setPaymentMethod(PaymentMethod.CHECK);

        Map<String, Object> summary = service.processBatchPayments(Arrays.asList(good, bad));

        assertEquals(2, summary.get("totalSubmitted"));
        assertEquals(1, summary.get("processed"));
        assertEquals(1, summary.get("failed"));
    }

    @Test
    void assessLateFeePublishesLateFeesAssessedWithCatalogPayload() {
        when(accountProjectionRepository.findByLoanId(10L))
                .thenReturn(Optional.of(new AccountProjection(99L, 10L, new BigDecimal("10000.00"))));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

        service.assessLateFee(10L, 45);

        verify(eventPublisher).publish(eventCaptor.capture());
        LateFeesAssessed event = (LateFeesAssessed) eventCaptor.getValue();
        assertEquals(10L, event.getLoanId());
        assertEquals(45, event.getDaysPastDue());
        // 5% of 10000 = 500, capped at MAX_LATE_FEE 50.00
        assertAmount("50.00", event.getFeeAmount());
    }

    @Test
    void assessLateFeeFlatUnder30Days() {
        when(accountProjectionRepository.findByLoanId(10L))
                .thenReturn(Optional.of(new AccountProjection(99L, 10L, new BigDecimal("10000.00"))));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

        service.assessLateFee(10L, 15);

        verify(eventPublisher).publish(eventCaptor.capture());
        LateFeesAssessed event = (LateFeesAssessed) eventCaptor.getValue();
        assertAmount("25.00", event.getFeeAmount());
    }
}
