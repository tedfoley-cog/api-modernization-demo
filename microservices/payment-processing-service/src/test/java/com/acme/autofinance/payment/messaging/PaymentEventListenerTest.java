package com.acme.autofinance.payment.messaging;

import com.acme.autofinance.events.AccountBalanceUpdated;
import com.acme.autofinance.events.AccountCreated;
import com.acme.autofinance.events.AccountDelinquent;
import com.acme.autofinance.events.LoanFunded;
import com.acme.autofinance.payment.domain.AccountProjection;
import com.acme.autofinance.payment.domain.LoanProjection;
import com.acme.autofinance.payment.domain.ProcessedEvent;
import com.acme.autofinance.payment.repository.AccountProjectionRepository;
import com.acme.autofinance.payment.repository.LoanProjectionRepository;
import com.acme.autofinance.payment.repository.ProcessedEventRepository;
import com.acme.autofinance.payment.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentEventListenerTest {

    @Mock private LoanProjectionRepository loanProjectionRepository;
    @Mock private AccountProjectionRepository accountProjectionRepository;
    @Mock private ProcessedEventRepository processedEventRepository;
    @Mock private PaymentService paymentService;

    private PaymentEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new PaymentEventListener(loanProjectionRepository, accountProjectionRepository,
                processedEventRepository, paymentService);
    }

    @Test
    void loanFundedPopulatesProjectionOnceForDuplicateDeliveries() {
        LoanFunded event = new LoanFunded(10L, "APP-10", LocalDate.now(),
                new BigDecimal("20000.00"), new BigDecimal("6.00"), 60);
        when(loanProjectionRepository.findById(10L)).thenReturn(Optional.empty());
        // First delivery not seen, second delivery already recorded.
        when(processedEventRepository.existsById(event.getEventId())).thenReturn(false, true);

        listener.on(event);
        listener.on(event);

        ArgumentCaptor<LoanProjection> captor = ArgumentCaptor.forClass(LoanProjection.class);
        verify(loanProjectionRepository, times(1)).save(captor.capture());
        assertEquals(10L, captor.getValue().getLoanId());
        assertEquals(new BigDecimal("6.00"), captor.getValue().getInterestRate());
        verify(processedEventRepository, times(1)).save(any(ProcessedEvent.class));
    }

    @Test
    void accountCreatedCreatesProjection() {
        AccountCreated event = new AccountCreated(99L, "ACC-99", 10L, "Jane", new BigDecimal("20000.00"));
        when(accountProjectionRepository.findById(99L)).thenReturn(Optional.empty());
        when(processedEventRepository.existsById(event.getEventId())).thenReturn(false);

        listener.on(event);

        ArgumentCaptor<AccountProjection> captor = ArgumentCaptor.forClass(AccountProjection.class);
        verify(accountProjectionRepository).save(captor.capture());
        assertEquals(99L, captor.getValue().getAccountId());
        assertEquals(10L, captor.getValue().getLoanId());
        assertEquals(new BigDecimal("20000.00"), captor.getValue().getCurrentBalance());
    }

    @Test
    void accountBalanceUpdatedUpdatesExistingProjection() {
        AccountBalanceUpdated event = new AccountBalanceUpdated(99L, new BigDecimal("20000.00"),
                new BigDecimal("18500.00"), new BigDecimal("1500.00"), "PAYMENT");
        AccountProjection existing = new AccountProjection(99L, 10L, new BigDecimal("20000.00"));
        when(accountProjectionRepository.findById(99L)).thenReturn(Optional.of(existing));
        when(processedEventRepository.existsById(event.getEventId())).thenReturn(false);

        listener.on(event);

        ArgumentCaptor<AccountProjection> captor = ArgumentCaptor.forClass(AccountProjection.class);
        verify(accountProjectionRepository).save(captor.capture());
        assertEquals(new BigDecimal("18500.00"), captor.getValue().getCurrentBalance());
    }

    @Test
    void accountBalanceUpdatedIgnoredWhenProjectionMissing() {
        AccountBalanceUpdated event = new AccountBalanceUpdated(99L, new BigDecimal("20000.00"),
                new BigDecimal("18500.00"), new BigDecimal("1500.00"), "PAYMENT");
        when(accountProjectionRepository.findById(99L)).thenReturn(Optional.empty());
        when(processedEventRepository.existsById(event.getEventId())).thenReturn(false);

        listener.on(event);

        verify(accountProjectionRepository, never()).save(any(AccountProjection.class));
    }

    @Test
    void accountDelinquentAssessesLateFeeOnceForDuplicateDeliveries() {
        AccountDelinquent event = new AccountDelinquent(99L, 10L, 45, "30-59", new BigDecimal("10000.00"));
        when(accountProjectionRepository.findById(99L)).thenReturn(Optional.empty());
        when(processedEventRepository.existsById(event.getEventId())).thenReturn(false, true);

        listener.on(event);
        listener.on(event);

        verify(accountProjectionRepository, times(1)).save(any(AccountProjection.class));
        verify(paymentService, times(1)).assessLateFee(10L, 45);
    }
}
