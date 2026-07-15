package com.acme.autofinance.dealer.controller;

import com.acme.autofinance.dealer.domain.DealPackage;
import com.acme.autofinance.dealer.domain.Dealer;
import com.acme.autofinance.dealer.service.DealerService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * REST API for the dealer-integration bounded context. Endpoint paths and response
 * shapes are preserved from the legacy monolith controller.
 */
@RestController
@RequestMapping("/api/dealers")
public class DealerController {

    private final DealerService dealerService;

    public DealerController(DealerService dealerService) {
        this.dealerService = dealerService;
    }

    @PostMapping("/deals")
    public ResponseEntity<DealPackage> submitDealPackage(@RequestBody DealPackage dealPackage) {
        if (dealPackage.getDealerId() == null) {
            throw new IllegalArgumentException("Dealer ID is required");
        }
        if (dealPackage.getVehicleVin() == null || dealPackage.getVehicleVin().trim().isEmpty()) {
            throw new IllegalArgumentException("Vehicle VIN is required");
        }
        DealPackage result = dealerService.submitDealPackage(dealPackage);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @GetMapping("/{dealerId}/settlement")
    public ResponseEntity<Map<String, Object>> getDealerSettlement(@PathVariable Long dealerId) {
        return ResponseEntity.ok(dealerService.getDealerSettlement(dealerId));
    }

    @GetMapping("/{dealerId}/inventory-financing")
    public ResponseEntity<Map<String, Object>> getInventoryFinancing(@PathVariable Long dealerId) {
        return ResponseEntity.ok(dealerService.getInventoryFinancing(dealerId));
    }

    @GetMapping
    public ResponseEntity<List<Dealer>> listDealers() {
        return ResponseEntity.ok(dealerService.getAllDealers());
    }
}
