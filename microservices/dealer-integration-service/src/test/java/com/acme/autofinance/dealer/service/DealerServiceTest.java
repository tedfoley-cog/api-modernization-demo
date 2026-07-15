package com.acme.autofinance.dealer.service;

import com.acme.autofinance.dealer.domain.DealLoanProjection;
import com.acme.autofinance.dealer.domain.DealPackage;
import com.acme.autofinance.dealer.domain.DealStatus;
import com.acme.autofinance.dealer.domain.Dealer;
import com.acme.autofinance.dealer.repository.DealLoanProjectionRepository;
import com.acme.autofinance.dealer.repository.DealPackageRepository;
import com.acme.autofinance.dealer.repository.DealerRepository;
import com.acme.autofinance.events.DealPackageSubmitted;
import com.acme.autofinance.events.DealerSettlementCalculated;
import com.acme.autofinance.events.DomainEvent;
import com.acme.autofinance.messaging.DomainEventPublisher;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DealerServiceTest {

    @Mock private DealerRepository dealerRepository;
    @Mock private DealPackageRepository dealPackageRepository;
    @Mock private DealLoanProjectionRepository dealLoanProjectionRepository;
    @Mock private DomainEventPublisher eventPublisher;
    @Captor private ArgumentCaptor<DomainEvent> eventCaptor;

    private DealerService service;

    @BeforeEach
    void setUp() {
        service = new DealerService(dealerRepository, dealPackageRepository,
                dealLoanProjectionRepository, eventPublisher);
    }

    private Dealer dealer(Long id, boolean active) {
        Dealer d = new Dealer();
        d.setId(id);
        d.setDealerCode("DLR-00" + id);
        d.setDealerName("Metro Auto Group");
        d.setActive(active);
        d.setTier("PLATINUM");
        d.setReserveRate(new BigDecimal("1.75"));
        d.setHoldbackPct(new BigDecimal("2.00"));
        return d;
    }

    private DealPackage request() {
        DealPackage d = new DealPackage();
        d.setDealerId(1L);
        d.setVehicleVin("1HGCM82633A004352");
        d.setSalePrice(new BigDecimal("32000.00"));
        d.setDownPayment(new BigDecimal("3500.00"));
        d.setTradeInValue(new BigDecimal("0.00"));
        return d;
    }

    private static void assertAmount(String expected, BigDecimal actual) {
        assertTrue(actual != null && new BigDecimal(expected).compareTo(actual) == 0,
                "expected " + expected + " but was " + actual);
    }

    @Test
    void submitPublishesExactlyOneDealPackageSubmittedAndNoLoanCall() {
        when(dealerRepository.findById(1L)).thenReturn(Optional.of(dealer(1L, true)));
        when(dealPackageRepository.save(any(DealPackage.class))).thenAnswer(i -> {
            DealPackage d = i.getArgument(0);
            d.setId(100L);
            return d;
        });

        DealPackage saved = service.submitDealPackage(request());

        // Deal is persisted as dealer-owned and NOT linked to any synchronously created loan.
        assertEquals(DealStatus.SUBMITTED, saved.getStatus());
        assertEquals(null, saved.getLoanId());
        assertTrue(saved.getDealNumber().startsWith("DL-"));
        verify(dealPackageRepository, times(1)).save(any(DealPackage.class));

        // Exactly one event, correctly populated.
        verify(eventPublisher, times(1)).publish(eventCaptor.capture());
        DealPackageSubmitted event = (DealPackageSubmitted) eventCaptor.getValue();
        assertEquals(saved.getDealNumber(), event.getDealNumber());
        assertEquals(1L, event.getDealerId());
        assertEquals("1HGCM82633A004352", event.getVehicleVin());
        assertAmount("32000.00", event.getSalePrice());
        assertAmount("3500.00", event.getDownPayment());
        assertAmount("0.00", event.getTradeInValue());

        // No local loan-status projection is read or written during submission.
        verifyNoInteractions(dealLoanProjectionRepository);
    }

    @Test
    void submitRejectsInactiveDealerWithoutPublishing() {
        when(dealerRepository.findById(1L)).thenReturn(Optional.of(dealer(1L, false)));

        assertThrows(IllegalArgumentException.class, () -> service.submitDealPackage(request()));

        verify(dealPackageRepository, never()).save(any(DealPackage.class));
        verify(eventPublisher, never()).publish(any(DomainEvent.class));
    }

    @Test
    void submitRejectsUnknownDealer() {
        when(dealerRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.submitDealPackage(request()));
        verify(eventPublisher, never()).publish(any(DomainEvent.class));
    }

    @Test
    void settlementAggregatesFundedDealsAndPublishesEvent() {
        Dealer dealer = dealer(1L, true);
        when(dealerRepository.findById(1L)).thenReturn(Optional.of(dealer));

        DealPackage funded = new DealPackage();
        funded.setDealerId(1L);
        funded.setStatus(DealStatus.FUNDED);
        funded.setDealerReserve(new BigDecimal("500.00"));
        funded.setHoldbackAmount(new BigDecimal("640.00"));

        DealPackage settled = new DealPackage();
        settled.setDealerId(1L);
        settled.setStatus(DealStatus.SETTLED);
        settled.setDealerReserve(new BigDecimal("418.75"));
        settled.setHoldbackAmount(new BigDecimal("410.00"));

        DealPackage submitted = new DealPackage();
        submitted.setDealerId(1L);
        submitted.setStatus(DealStatus.SUBMITTED);
        submitted.setDealerReserve(new BigDecimal("999.00"));

        when(dealPackageRepository.findByDealerId(1L))
                .thenReturn(Arrays.asList(funded, settled, submitted));

        Map<String, Object> result = service.getDealerSettlement(1L);

        assertEquals(2, result.get("fundedDeals"));
        assertAmount("918.75", (BigDecimal) result.get("totalReserves"));
        assertAmount("1050.00", (BigDecimal) result.get("totalHoldbacks"));
        assertAmount("-131.25", (BigDecimal) result.get("netSettlement"));

        verify(eventPublisher, times(1)).publish(eventCaptor.capture());
        DealerSettlementCalculated event = (DealerSettlementCalculated) eventCaptor.getValue();
        assertEquals(1L, event.getDealerId());
        assertEquals("DLR-001", event.getDealerCode());
        assertAmount("918.75", event.getTotalReserves());
        assertAmount("1050.00", event.getTotalHoldbacks());
        assertAmount("-131.25", event.getNetSettlement());
    }

    @Test
    void creditApprovalAdvancesDealAndComputesReserveHoldback() {
        DealPackage deal = new DealPackage();
        deal.setDealerId(1L);
        deal.setVehicleVin("VIN1");
        deal.setStatus(DealStatus.UNDER_REVIEW);
        deal.setSalePrice(new BigDecimal("32000.00"));

        when(dealPackageRepository.findByVehicleVin("VIN1")).thenReturn(Collections.singletonList(deal));
        when(dealerRepository.findById(1L)).thenReturn(Optional.of(dealer(1L, true)));

        service.advanceOnCreditDecision("VIN1", "APPROVED", new BigDecimal("28500.00"));

        assertEquals(DealStatus.APPROVED, deal.getStatus());
        // reserve = 28500 * 1.75% = 498.75; holdback = 32000 * 2.00% = 640.00
        assertAmount("498.75", deal.getDealerReserve());
        assertAmount("640.00", deal.getHoldbackAmount());
        verify(dealPackageRepository).save(deal);
    }

    @Test
    void creditDeclineRejectsDeal() {
        DealPackage deal = new DealPackage();
        deal.setDealerId(1L);
        deal.setVehicleVin("VIN1");
        deal.setStatus(DealStatus.UNDER_REVIEW);

        when(dealPackageRepository.findByVehicleVin("VIN1")).thenReturn(Collections.singletonList(deal));

        service.advanceOnCreditDecision("VIN1", "DECLINED", null);

        assertEquals(DealStatus.REJECTED, deal.getStatus());
    }

    @Test
    void loanFundedAdvancesApprovedDealToFunded() {
        DealPackage deal = new DealPackage();
        deal.setDealerId(1L);
        deal.setVehicleVin("VIN1");
        deal.setStatus(DealStatus.APPROVED);

        when(dealPackageRepository.findByVehicleVin("VIN1")).thenReturn(Collections.singletonList(deal));

        service.advanceOnLoanFunded("VIN1", 55L);

        assertEquals(DealStatus.FUNDED, deal.getStatus());
        assertEquals(55L, deal.getLoanId());
    }

    @Test
    void inventoryFinancingUsesLocalProjectionOnly() {
        when(dealerRepository.findById(1L)).thenReturn(Optional.of(dealer(1L, true)));

        DealLoanProjection active = new DealLoanProjection(1L, "LN-1", 1L, "VIN1", "ACTIVE", new BigDecimal("28500.00"));
        DealLoanProjection funded = new DealLoanProjection(2L, "LN-2", 1L, "VIN2", "FUNDED", new BigDecimal("15000.00"));
        DealLoanProjection declined = new DealLoanProjection(3L, "LN-3", 1L, "VIN3", "DECLINED", null);
        when(dealLoanProjectionRepository.findByDealerId(1L))
                .thenReturn(Arrays.asList(active, funded, declined));

        Map<String, Object> financing = service.getInventoryFinancing(1L);

        assertEquals(2, financing.get("activeLoans"));
        assertAmount("43500.00", (BigDecimal) financing.get("activeBalance"));
        assertAmount("5000000", (BigDecimal) financing.get("floorPlanLimit"));
        assertAmount("4956500.00", (BigDecimal) financing.get("availableCredit"));

        // Inventory financing must never publish or mutate deals.
        verify(eventPublisher, never()).publish(any(DomainEvent.class));
    }
}
