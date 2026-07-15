package com.acme.autofinance.dealer.messaging;

import com.acme.autofinance.dealer.domain.DealLoanProjection;
import com.acme.autofinance.dealer.domain.ProcessedEvent;
import com.acme.autofinance.dealer.repository.DealLoanProjectionRepository;
import com.acme.autofinance.dealer.repository.ProcessedEventRepository;
import com.acme.autofinance.dealer.service.DealerService;
import com.acme.autofinance.events.CreditDecisionMade;
import com.acme.autofinance.events.LoanApplicationSubmitted;
import com.acme.autofinance.events.LoanFunded;
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
class DealerEventListenerTest {

    @Mock private DealLoanProjectionRepository dealLoanProjectionRepository;
    @Mock private ProcessedEventRepository processedEventRepository;
    @Mock private DealerService dealerService;

    private DealerEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new DealerEventListener(dealLoanProjectionRepository,
                processedEventRepository, dealerService);
    }

    @Test
    void loanApplicationSubmittedCreatesProjectionAndAdvancesDealOnce() {
        LoanApplicationSubmitted event = new LoanApplicationSubmitted(
                10L, "LN-10", "John Smith", new BigDecimal("28500.00"), "VIN1", 1L);
        when(dealLoanProjectionRepository.findById(10L)).thenReturn(Optional.empty());
        // First delivery not seen, second delivery already recorded.
        when(processedEventRepository.existsById(event.getEventId())).thenReturn(false, true);

        listener.on(event);
        listener.on(event);

        ArgumentCaptor<DealLoanProjection> captor = ArgumentCaptor.forClass(DealLoanProjection.class);
        verify(dealLoanProjectionRepository, times(1)).save(captor.capture());
        assertEquals(10L, captor.getValue().getApplicationId());
        assertEquals("VIN1", captor.getValue().getVehicleVin());
        assertEquals("SUBMITTED", captor.getValue().getLoanStatus());
        verify(dealerService, times(1)).advanceOnApplicationSubmitted("VIN1");
        verify(processedEventRepository, times(1)).save(any(ProcessedEvent.class));
    }

    @Test
    void creditDecisionUpdatesProjectionAndAdvancesDeal() {
        CreditDecisionMade event = new CreditDecisionMade(
                10L, "APPROVED", 745, "PRIME", new BigDecimal("28500.00"), new BigDecimal("3.99"));
        DealLoanProjection projection =
                new DealLoanProjection(10L, "LN-10", 1L, "VIN1", "SUBMITTED", null);
        when(dealLoanProjectionRepository.findById(10L)).thenReturn(Optional.of(projection));
        when(processedEventRepository.existsById(event.getEventId())).thenReturn(false);

        listener.on(event);

        assertEquals("APPROVED", projection.getLoanStatus());
        assertEquals(new BigDecimal("28500.00"), projection.getApprovedAmount());
        verify(dealLoanProjectionRepository).save(projection);
        verify(dealerService).advanceOnCreditDecision("VIN1", "APPROVED", new BigDecimal("28500.00"));
    }

    @Test
    void creditDecisionIgnoredWhenProjectionMissing() {
        CreditDecisionMade event = new CreditDecisionMade(
                10L, "APPROVED", 745, "PRIME", new BigDecimal("28500.00"), new BigDecimal("3.99"));
        when(dealLoanProjectionRepository.findById(10L)).thenReturn(Optional.empty());
        when(processedEventRepository.existsById(event.getEventId())).thenReturn(false);

        listener.on(event);

        verify(dealLoanProjectionRepository, never()).save(any(DealLoanProjection.class));
        verify(dealerService, never()).advanceOnCreditDecision(any(), any(), any());
    }

    @Test
    void loanFundedLinksLoanAndAdvancesDealOnce() {
        LoanFunded event = new LoanFunded(55L, "LN-10", LocalDate.now(),
                new BigDecimal("28500.00"), new BigDecimal("3.99"), 60);
        DealLoanProjection projection =
                new DealLoanProjection(10L, "LN-10", 1L, "VIN1", "APPROVED", new BigDecimal("28500.00"));
        when(dealLoanProjectionRepository.findByApplicationNumber("LN-10"))
                .thenReturn(Optional.of(projection));
        when(processedEventRepository.existsById(event.getEventId())).thenReturn(false, true);

        listener.on(event);
        listener.on(event);

        assertEquals(55L, projection.getLoanId());
        assertEquals("FUNDED", projection.getLoanStatus());
        verify(dealLoanProjectionRepository, times(1)).save(projection);
        verify(dealerService, times(1)).advanceOnLoanFunded("VIN1", 55L);
    }
}
