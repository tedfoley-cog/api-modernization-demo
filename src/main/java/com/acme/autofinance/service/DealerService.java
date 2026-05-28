package com.acme.autofinance.service;

import com.acme.autofinance.model.DealPackage;
import com.acme.autofinance.model.DealStatus;
import com.acme.autofinance.model.Dealer;
import com.acme.autofinance.model.LoanApplication;
import com.acme.autofinance.model.LoanStatus;
import com.acme.autofinance.repository.DealPackageRepository;
import com.acme.autofinance.repository.DealerRepository;
import com.acme.autofinance.repository.LoanRepository;
import org.springframework.beans.factory.annotation.Autowired;
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
 * Dealer integration — tightly coupled to loan origination.
 * Deal submission directly creates loan applications and calculates reserves/holdbacks.
 */
@Service
public class DealerService {

    @Autowired
    private DealerRepository dealerRepository;

    @Autowired
    private DealPackageRepository dealPackageRepository;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private LoanService loanService;

    @Transactional
    public DealPackage submitDealPackage(DealPackage dealPackage) {
        dealPackage.setDealNumber("DL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        dealPackage.setStatus(DealStatus.SUBMITTED);
        dealPackage.setSubmissionDate(new Date());

        // Validate dealer exists — direct cross-domain check
        Dealer dealer = dealerRepository.findById(dealPackage.getDealerId())
                .orElseThrow(() -> new RuntimeException("Dealer not found: " + dealPackage.getDealerId()));

        if (!dealer.getActive()) {
            throw new RuntimeException("Dealer is not active: " + dealer.getDealerCode());
        }

        DealPackage saved = dealPackageRepository.save(dealPackage);

        // Create loan application from deal — tight coupling to origination
        LoanApplication loanApp = new LoanApplication();
        loanApp.setVehicleVin(dealPackage.getVehicleVin());
        loanApp.setRequestedAmount(
                dealPackage.getSalePrice()
                        .subtract(dealPackage.getDownPayment() != null ? dealPackage.getDownPayment() : BigDecimal.ZERO)
                        .subtract(dealPackage.getTradeInValue() != null ? dealPackage.getTradeInValue() : BigDecimal.ZERO));
        loanApp.setDealerId(dealPackage.getDealerId());
        loanApp.setTermMonths(60);

        // Synchronously create and process the loan application
        LoanApplication createdLoan = loanService.createApplication(loanApp);
        saved.setLoanId(createdLoan.getId());
        saved.setStatus(DealStatus.UNDER_REVIEW);

        return dealPackageRepository.save(saved);
    }

    public Map<String, Object> getDealerSettlement(Long dealerId) {
        Dealer dealer = dealerRepository.findById(dealerId)
                .orElseThrow(() -> new RuntimeException("Dealer not found: " + dealerId));

        Map<String, Object> settlement = new HashMap<>();
        settlement.put("dealerCode", dealer.getDealerCode());
        settlement.put("dealerName", dealer.getDealerName());

        // Calculate total reserves and holdbacks — cross-domain query
        List<DealPackage> fundedDeals = dealPackageRepository.findByDealerId(dealerId);
        BigDecimal totalReserves = BigDecimal.ZERO;
        BigDecimal totalHoldbacks = BigDecimal.ZERO;
        int fundedCount = 0;

        for (DealPackage deal : fundedDeals) {
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

        settlement.put("fundedDeals", fundedCount);
        settlement.put("totalReserves", totalReserves);
        settlement.put("totalHoldbacks", totalHoldbacks);
        settlement.put("netSettlement", totalReserves.subtract(totalHoldbacks));
        settlement.put("settlementDate", new Date());

        // Inline reserve rate calculation — should use dealer's configured rate
        BigDecimal reserveRate = dealer.getReserveRate() != null ? dealer.getReserveRate() : new BigDecimal("1.50");
        settlement.put("currentReserveRate", reserveRate);

        return settlement;
    }

    public Map<String, Object> getInventoryFinancing(Long dealerId) {
        Dealer dealer = dealerRepository.findById(dealerId)
                .orElseThrow(() -> new RuntimeException("Dealer not found: " + dealerId));

        Map<String, Object> financing = new HashMap<>();
        financing.put("dealerCode", dealer.getDealerCode());
        financing.put("tier", dealer.getTier());

        // Cross-domain query: count active loans originated by this dealer
        List<LoanApplication> dealerLoans = loanRepository.findByDealerId(dealerId);
        int activeCount = 0;
        BigDecimal activeBalance = BigDecimal.ZERO;
        for (LoanApplication loan : dealerLoans) {
            if (loan.getStatus() == LoanStatus.ACTIVE || loan.getStatus() == LoanStatus.FUNDED) {
                activeCount++;
                if (loan.getApprovedAmount() != null) {
                    activeBalance = activeBalance.add(loan.getApprovedAmount());
                }
            }
        }

        financing.put("activeLoans", activeCount);
        financing.put("activeBalance", activeBalance);
        financing.put("ytdVolume", dealer.getYtdVolume());

        // Hardcoded floor plan limit by tier
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
}
