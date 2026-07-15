package com.acme.autofinance.reporting.controller;

import com.acme.autofinance.reporting.service.ReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

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
