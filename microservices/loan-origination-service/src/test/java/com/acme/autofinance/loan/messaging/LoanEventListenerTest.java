package com.acme.autofinance.loan.messaging;

import com.acme.autofinance.events.DealPackageSubmitted;
import com.acme.autofinance.events.PaymentProcessed;
import com.acme.autofinance.loan.domain.ProcessedEvent;
import com.acme.autofinance.loan.repository.ProcessedEventRepository;
import com.acme.autofinance.loan.service.LoanService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoanEventListenerTest {

    @Mock private LoanService loanService;
    @Mock private ProcessedEventRepository processedEventRepository;

    private LoanEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new LoanEventListener(loanService, processedEventRepository);
    }

    @Test
    void dealPackageSubmittedOriginatesLoanOnceForDuplicateDeliveries() {
        DealPackageSubmitted event = new DealPackageSubmitted("DEAL-9", 7L, "1FTFW1E50NFA10001",
                new BigDecimal("35000.00"), new BigDecimal("5000.00"), new BigDecimal("2000.00"));
        // First delivery not seen, second delivery already recorded (idempotent inbox).
        when(processedEventRepository.existsById(event.getEventId())).thenReturn(false, true);

        listener.on(event);
        listener.on(event);

        verify(loanService, times(1)).createApplicationFromDealSubmission(
                eq("DEAL-9"), eq(7L), eq("1FTFW1E50NFA10001"),
                eq(new BigDecimal("35000.00")), eq(new BigDecimal("5000.00")), eq(new BigDecimal("2000.00")));
        verify(processedEventRepository, times(1)).save(any(ProcessedEvent.class));
    }

    @Test
    void paymentProcessedProjectsLifecycleOnceForDuplicateDeliveries() {
        PaymentProcessed event = new PaymentProcessed(5L, 1001L, LocalDate.now(),
                "COMPLETED", new BigDecimal("400.00"), BigDecimal.ZERO);
        when(processedEventRepository.existsById(event.getEventId())).thenReturn(false, true);

        listener.on(event);
        listener.on(event);

        verify(loanService, times(1)).projectPaymentProcessed(1001L, BigDecimal.ZERO);
    }
}
