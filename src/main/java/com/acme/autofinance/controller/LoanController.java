package com.acme.autofinance.controller;

import com.acme.autofinance.model.LoanApplication;
import com.acme.autofinance.model.LoanStatus;
import com.acme.autofinance.service.LoanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/loans")
public class LoanController {

    @Autowired
    private LoanService loanService;

    @PostMapping
    public ResponseEntity<LoanApplication> createApplication(@RequestBody LoanApplication application) {
        // Controller does validation inline — should be in a validator or service
        if (application.getApplicantName() == null || application.getApplicantName().trim().isEmpty()) {
            throw new RuntimeException("Applicant name is required");
        }
        if (application.getRequestedAmount() == null || application.getRequestedAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Requested amount must be positive");
        }
        LoanApplication created = loanService.createApplication(application);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<LoanApplication> getApplication(@PathVariable Long id) {
        return ResponseEntity.ok(loanService.getApplication(id));
    }

    @GetMapping("/number/{applicationNumber}")
    public ResponseEntity<LoanApplication> getByNumber(@PathVariable String applicationNumber) {
        return ResponseEntity.ok(loanService.getByApplicationNumber(applicationNumber));
    }

    @GetMapping
    public ResponseEntity<List<LoanApplication>> listApplications(
            @RequestParam(required = false) String status) {
        if (status != null) {
            return ResponseEntity.ok(loanService.getByStatus(LoanStatus.valueOf(status)));
        }
        return ResponseEntity.ok(loanService.getAllApplications());
    }

    @PutMapping("/{id}/terms")
    public ResponseEntity<LoanApplication> updateTerms(
            @PathVariable Long id,
            @RequestParam(required = false) BigDecimal amount,
            @RequestParam(required = false) BigDecimal rate,
            @RequestParam(required = false) Integer termMonths) {
        return ResponseEntity.ok(loanService.updateTerms(id, amount, rate, termMonths));
    }

    @PostMapping("/{id}/fund")
    public ResponseEntity<LoanApplication> fundLoan(@PathVariable Long id) {
        return ResponseEntity.ok(loanService.fundLoan(id));
    }

    @PostMapping("/end-of-day")
    public ResponseEntity<Map<String, Object>> endOfDayProcessing() {
        return ResponseEntity.ok(loanService.runEndOfDayProcessing());
    }
}
