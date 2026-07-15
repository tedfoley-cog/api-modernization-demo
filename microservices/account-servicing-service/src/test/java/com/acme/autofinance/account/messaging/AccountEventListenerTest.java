package com.acme.autofinance.account.messaging;

import com.acme.autofinance.account.domain.ProcessedEvent;
import com.acme.autofinance.account.repository.ProcessedEventRepository;
import com.acme.autofinance.account.service.AccountService;
import com.acme.autofinance.events.LoanFunded;
import com.acme.autofinance.events.PaymentAllocated;
import com.acme.autofinance.events.PaymentProcessed;
import com.acme.autofinance.events.PaymentReceived;
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
class AccountEventListenerTest {

    @Mock private AccountService accountService;
    @Mock private ProcessedEventRepository processedEventRepository;

    private AccountEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new AccountEventListener(accountService, processedEventRepository);
    }

    @Test
    void loanFundedTriggersAccountCreationOnceForDuplicateDeliveries() {
        LoanFunded event = new LoanFunded(1001L, "APP-1001", LocalDate.now(),
                new BigDecimal("20000.00"), new BigDecimal("6.00"), 60);
        // First delivery not seen, second delivery already recorded (idempotent inbox).
        when(processedEventRepository.existsById(event.getEventId())).thenReturn(false, true);

        listener.on(event);
        listener.on(event);

        verify(accountService, times(1)).createAccountForLoan(
                eq(1001L), eq(null), eq(new BigDecimal("20000.00")), eq(new BigDecimal("6.00")), eq(60));
        verify(processedEventRepository, times(1)).save(any(ProcessedEvent.class));
    }

    @Test
    void paymentProcessedAppliesBalanceOnceForDuplicateDeliveries() {
        PaymentProcessed event = new PaymentProcessed(5L, 1001L, LocalDate.now(),
                "COMPLETED", new BigDecimal("400.00"), new BigDecimal("9600.00"));
        when(processedEventRepository.existsById(event.getEventId())).thenReturn(false, true);

        listener.on(event);
        listener.on(event);

        // Duplicate delivery must not double-apply the balance.
        verify(accountService, times(1)).applyProcessedPayment(1001L, new BigDecimal("400.00"));
    }

    @Test
    void paymentReceivedRecordedOncePerEventId() {
        PaymentReceived event = new PaymentReceived(5L, 1001L, new BigDecimal("450.00"),
                "ACH", "PMT-ABC123");
        when(processedEventRepository.existsById(event.getEventId())).thenReturn(false, true);

        listener.on(event);
        listener.on(event);

        verify(accountService, times(1)).recordPaymentReceived(eq(1001L), any());
    }

    @Test
    void paymentAllocatedReducesFeesOncePerEventId() {
        PaymentAllocated event = new PaymentAllocated(5L, 1001L, new BigDecimal("400.00"),
                new BigDecimal("50.00"), new BigDecimal("25.00"));
        when(processedEventRepository.existsById(event.getEventId())).thenReturn(false, true);

        listener.on(event);
        listener.on(event);

        verify(accountService, times(1)).applyFeeAllocation(1001L, new BigDecimal("25.00"));
    }
}
