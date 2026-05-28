package com.acme.autofinance.controller;

import com.acme.autofinance.service.LoanService;
import com.acme.autofinance.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    @Autowired
    private ReportService reportService;

    @Autowired
    private LoanService loanService;

    @GetMapping("/portfolio")
    public ResponseEntity<Map<String, Object>> getPortfolioSummary() {
        return ResponseEntity.ok(reportService.getPortfolioSummary());
    }

    @GetMapping("/delinquency")
    public ResponseEntity<Map<String, Object>> getDelinquencyReport() {
        return ResponseEntity.ok(reportService.getDelinquencyReport());
    }

    @GetMapping("/dealers")
    public ResponseEntity<List<Map<String, Object>>> getDealerPerformance() {
        return ResponseEntity.ok(reportService.getDealerPerformanceReport());
    }
}
