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
import com.acme.autofinance.messaging.DomainEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Dealer integration service for the dealer-integration bounded context. Owns the
 * dealer and deal-package aggregates. Deal submission persists the dealer-owned deal
 * and publishes {@link DealPackageSubmitted} instead of synchronously creating a
 * loan — loan origination reacts to that event. Deal status is advanced
 * asynchronously as loan events arrive, using only dealer-owned and local
 * projection data. This service holds no loan repository, service, or entity.
 */
@Service
public class DealerService {

    private final DealerRepository dealerRepository;
    private final DealPackageRepository dealPackageRepository;
    private final DealLoanProjectionRepository dealLoanProjectionRepository;
    private final DomainEventPublisher eventPublisher;

    public DealerService(DealerRepository dealerRepository,
                         DealPackageRepository dealPackageRepository,
                         DealLoanProjectionRepository dealLoanProjectionRepository,
                         DomainEventPublisher eventPublisher) {
        this.dealerRepository = dealerRepository;
        this.dealPackageRepository = dealPackageRepository;
        this.dealLoanProjectionRepository = dealLoanProjectionRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public DealPackage submitDealPackage(DealPackage dealPackage) {
        Dealer dealer = dealerRepository.findById(dealPackage.getDealerId())
                .orElseThrow(() -> new IllegalArgumentException("Dealer not found: " + dealPackage.getDealerId()));
        if (dealer.getActive() == null || !dealer.getActive()) {
            throw new IllegalArgumentException("Dealer is not active: " + dealer.getDealerCode());
        }

        dealPackage.setDealNumber("DL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        dealPackage.setStatus(DealStatus.SUBMITTED);
        dealPackage.setSubmissionDate(new Date());
        dealPackage.setLoanId(null);

        DealPackage saved = dealPackageRepository.save(dealPackage);

        // Publish the deal package instead of synchronously creating a loan.
        eventPublisher.publish(new DealPackageSubmitted(
                saved.getDealNumber(),
                saved.getDealerId(),
                saved.getVehicleVin(),
                saved.getSalePrice(),
                saved.getDownPayment(),
                saved.getTradeInValue()));

        return saved;
    }

    @Transactional
    public Map<String, Object> getDealerSettlement(Long dealerId) {
        Dealer dealer = dealerRepository.findById(dealerId)
                .orElseThrow(() -> new IllegalArgumentException("Dealer not found: " + dealerId));

        Map<String, Object> settlement = new HashMap<>();
        settlement.put("dealerCode", dealer.getDealerCode());
        settlement.put("dealerName", dealer.getDealerName());

        List<DealPackage> deals = dealPackageRepository.findByDealerId(dealerId);
        BigDecimal totalReserves = BigDecimal.ZERO;
        BigDecimal totalHoldbacks = BigDecimal.ZERO;
        int fundedCount = 0;

        for (DealPackage deal : deals) {
            if (deal.getStatus() == DealStatus.FUNDED || deal.getStatus() == DealStatus.SETTLED) {
                if (deal.getDealerReserve() != null) {
                    totalReserves = totalReserves.add(deal.getDealerReserve());
                }
                if (deal.getHoldbackAmount() != null) {
                    totalHoldbacks = totalHoldbacks.add(deal.getHoldbackAmount());
                }
                fundedCount++;
            }
        }

        BigDecimal netSettlement = totalReserves.subtract(totalHoldbacks);
        BigDecimal reserveRate = dealer.getReserveRate() != null ? dealer.getReserveRate() : new BigDecimal("1.50");

        settlement.put("fundedDeals", fundedCount);
        settlement.put("totalReserves", totalReserves);
        settlement.put("totalHoldbacks", totalHoldbacks);
        settlement.put("netSettlement", netSettlement);
        settlement.put("currentReserveRate", reserveRate);
        settlement.put("settlementDate", new Date());

        eventPublisher.publish(new DealerSettlementCalculated(
                dealer.getId(),
                dealer.getDealerCode(),
                totalReserves,
                totalHoldbacks,
                netSettlement));

        return settlement;
    }

    /**
     * Inventory financing computed from dealer-owned and local projection data only.
     * Active loan counts/balances come from the event-sourced {@link DealLoanProjection},
     * never from the loan-origination database.
     */
    public Map<String, Object> getInventoryFinancing(Long dealerId) {
        Dealer dealer = dealerRepository.findById(dealerId)
                .orElseThrow(() -> new IllegalArgumentException("Dealer not found: " + dealerId));

        Map<String, Object> financing = new HashMap<>();
        financing.put("dealerCode", dealer.getDealerCode());
        financing.put("tier", dealer.getTier());

        List<DealLoanProjection> dealerLoans = dealLoanProjectionRepository.findByDealerId(dealerId);
        int activeCount = 0;
        BigDecimal activeBalance = BigDecimal.ZERO;
        for (DealLoanProjection loan : dealerLoans) {
            if ("ACTIVE".equals(loan.getLoanStatus()) || "FUNDED".equals(loan.getLoanStatus())) {
                activeCount++;
                if (loan.getApprovedAmount() != null) {
                    activeBalance = activeBalance.add(loan.getApprovedAmount());
                }
            }
        }

        financing.put("activeLoans", activeCount);
        financing.put("activeBalance", activeBalance);
        financing.put("ytdVolume", dealer.getYtdVolume());

        BigDecimal floorPlanLimit;
        if ("PLATINUM".equals(dealer.getTier())) {
            floorPlanLimit = new BigDecimal("5000000");
        } else if ("GOLD".equals(dealer.getTier())) {
            floorPlanLimit = new BigDecimal("2500000");
        } else {
            floorPlanLimit = new BigDecimal("1000000");
        }

        financing.put("floorPlanLimit", floorPlanLimit);
        financing.put("availableCredit", floorPlanLimit.subtract(activeBalance));

        return financing;
    }

    public List<Dealer> getAllDealers() {
        return dealerRepository.findAll();
    }

    /** Advances matching deals to UNDER_REVIEW when their loan application is submitted. */
    @Transactional
    public void advanceOnApplicationSubmitted(String vehicleVin) {
        if (vehicleVin == null) {
            return;
        }
        for (DealPackage deal : dealPackageRepository.findByVehicleVin(vehicleVin)) {
            if (deal.getStatus() == DealStatus.SUBMITTED) {
                deal.setStatus(DealStatus.UNDER_REVIEW);
                dealPackageRepository.save(deal);
            }
        }
    }

    /**
     * Advances matching deals on a credit decision: APPROVED deals accrue dealer
     * reserve/holdback computed from dealer-owned rates; declined deals are rejected.
     */
    @Transactional
    public void advanceOnCreditDecision(String vehicleVin, String decision, BigDecimal approvedAmount) {
        if (vehicleVin == null) {
            return;
        }
        boolean approved = "APPROVED".equalsIgnoreCase(decision);
        for (DealPackage deal : dealPackageRepository.findByVehicleVin(vehicleVin)) {
            if (deal.getStatus() != DealStatus.SUBMITTED && deal.getStatus() != DealStatus.UNDER_REVIEW) {
                continue;
            }
            if (approved) {
                deal.setStatus(DealStatus.APPROVED);
                applyReserveAndHoldback(deal, approvedAmount);
            } else {
                deal.setStatus(DealStatus.REJECTED);
            }
            dealPackageRepository.save(deal);
        }
    }

    /** Advances matching approved deals to FUNDED and records the funded loan id. */
    @Transactional
    public void advanceOnLoanFunded(String vehicleVin, Long loanId) {
        if (vehicleVin == null) {
            return;
        }
        for (DealPackage deal : dealPackageRepository.findByVehicleVin(vehicleVin)) {
            if (deal.getStatus() == DealStatus.APPROVED) {
                deal.setStatus(DealStatus.FUNDED);
                deal.setLoanId(loanId);
                dealPackageRepository.save(deal);
            }
        }
    }

    private void applyReserveAndHoldback(DealPackage deal, BigDecimal approvedAmount) {
        Dealer dealer = dealerRepository.findById(deal.getDealerId()).orElse(null);
        if (dealer == null) {
            return;
        }
        BigDecimal reserveBase = approvedAmount != null ? approvedAmount : deal.getSalePrice();
        if (reserveBase != null && dealer.getReserveRate() != null) {
            deal.setDealerReserve(reserveBase.multiply(dealer.getReserveRate())
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP));
        }
        if (deal.getSalePrice() != null && dealer.getHoldbackPct() != null) {
            deal.setHoldbackAmount(deal.getSalePrice().multiply(dealer.getHoldbackPct())
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP));
        }
    }
}
