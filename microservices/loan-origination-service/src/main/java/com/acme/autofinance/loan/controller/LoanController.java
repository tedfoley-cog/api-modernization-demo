package com.acme.autofinance.loan.controller;

import com.acme.autofinance.loan.domain.LoanApplication;
import com.acme.autofinance.loan.domain.LoanStatus;
import com.acme.autofinance.loan.service.LoanService;
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

/**
 * REST API for the loan-origination bounded context. Preserves the loan-owned
 * endpoints from the legacy monolith controller. The cross-domain
 * {@code POST /api/loans/end-of-day} batch endpoint is intentionally not carried
 * over — end-of-day account processing belongs to account servicing, not loan
 * origination.
 */
@RestController
@RequestMapping("/api/loans")
public class LoanController {

    private final LoanService loanService;

    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    @PostMapping
    public ResponseEntity<LoanApplication> createApplication(@RequestBody LoanApplication application) {
        if (application.getApplicantName() == null || application.getApplicantName().trim().isEmpty()) {
            throw new IllegalArgumentException("Applicant name is required");
        }
        if (application.getRequestedAmount() == null
                || application.getRequestedAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Requested amount must be positive");
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
}
