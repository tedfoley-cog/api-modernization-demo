package com.acme.autofinance.controller;

import com.acme.autofinance.model.DealPackage;
import com.acme.autofinance.model.Dealer;
import com.acme.autofinance.service.DealerService;
import org.springframework.beans.factory.annotation.Autowired;
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

@RestController
@RequestMapping("/api/dealers")
public class DealerController {

    @Autowired
    private DealerService dealerService;

    @PostMapping("/deals")
    public ResponseEntity<DealPackage> submitDealPackage(@RequestBody DealPackage dealPackage) {
        // Inline validation — mixed concerns
        if (dealPackage.getDealerId() == null) {
            throw new RuntimeException("Dealer ID is required");
        }
        if (dealPackage.getVehicleVin() == null || dealPackage.getVehicleVin().trim().isEmpty()) {
            throw new RuntimeException("Vehicle VIN is required");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(dealerService.submitDealPackage(dealPackage));
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
