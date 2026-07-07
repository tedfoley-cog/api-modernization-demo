package com.acme.payment.controller;

import com.acme.payment.domain.event.PaymentEvent;
import com.acme.payment.config.EventBusConfig;
import com.acme.payment.domain.model.Payment;
import com.acme.payment.domain.model.PaymentSummary;
import com.acme.payment.service.LateFeeService;
import com.acme.payment.service.PaymentCalculationService;
import com.acme.payment.service.PaymentCommandService;
import com.acme.payment.service.PaymentQueryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v2/payments")
public class PaymentController {

    private final PaymentCommandService commandService;
    private final PaymentQueryService queryService;
    private final PaymentCalculationService calculationService;
    private final LateFeeService lateFeeService;
    private final EventBusConfig.EventStore eventStore;

    public PaymentController(PaymentCommandService commandService,
                             PaymentQueryService queryService,
                             PaymentCalculationService calculationService,
                             LateFeeService lateFeeService,
                             EventBusConfig.EventStore eventStore) {
        this.commandService = commandService;
        this.queryService = queryService;
        this.calculationService = calculationService;
        this.lateFeeService = lateFeeService;
        this.eventStore = eventStore;
    }

    @PostMapping
    public ResponseEntity<Payment> submitPayment(
            @RequestBody Payment payment,
            @RequestParam(defaultValue = "0") BigDecimal currentBalance,
            @RequestParam(defaultValue = "0") BigDecimal annualRate) {
        if (payment.getLoanId() == null) {
            throw new RuntimeException("Loan ID is required");
        }
        if (payment.getPaymentAmount() == null || payment.getPaymentAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Payment amount must be positive");
        }
        Payment result = commandService.submitPayment(payment, currentBalance, annualRate);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @GetMapping("/loan/{loanId}")
    public ResponseEntity<List<Payment>> getPaymentHistory(@PathVariable Long loanId) {
        return ResponseEntity.ok(queryService.getPaymentHistory(loanId));
    }

    @PostMapping("/batch")
    public ResponseEntity<Map<String, Object>> processBatch(
            @RequestBody List<Payment> payments,
            @RequestParam(defaultValue = "0") BigDecimal defaultBalance,
            @RequestParam(defaultValue = "0") BigDecimal defaultRate) {
        return ResponseEntity.ok(commandService.processBatchPayments(payments, defaultBalance, defaultRate));
    }

    @GetMapping("/pending")
    public ResponseEntity<List<Payment>> getPendingPayments() {
        return ResponseEntity.ok(queryService.getPendingPayments());
    }

    // --- CQRS Read Model Endpoints ---

    @GetMapping("/summary/{loanId}")
    public ResponseEntity<PaymentSummary> getPaymentSummary(@PathVariable Long loanId) {
        Optional<PaymentSummary> summary = queryService.getPaymentSummary(loanId);
        return summary.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/summary")
    public ResponseEntity<List<PaymentSummary>> getAllSummaries() {
        return ResponseEntity.ok(queryService.getAllSummaries());
    }

    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> getPaymentMetrics() {
        return ResponseEntity.ok(queryService.getPaymentMetrics());
    }

    @GetMapping("/status-breakdown")
    public ResponseEntity<Map<String, Long>> getStatusBreakdown() {
        return ResponseEntity.ok(queryService.getPaymentStatusBreakdown());
    }

    // --- Calculation Endpoints ---

    @PostMapping("/calculate-monthly")
    public ResponseEntity<Map<String, Object>> calculateMonthly(
            @RequestParam BigDecimal principal,
            @RequestParam BigDecimal annualRate,
            @RequestParam int termMonths) {
        BigDecimal monthly = calculationService.calculateMonthlyPayment(principal, annualRate, termMonths);
        Map<String, Object> result = new HashMap<>();
        result.put("principal", principal);
        result.put("annualRate", annualRate);
        result.put("termMonths", termMonths);
        result.put("monthlyPayment", monthly);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/calculate-payoff")
    public ResponseEntity<Map<String, Object>> calculatePayoff(
            @RequestParam BigDecimal approvedAmount,
            @RequestParam BigDecimal interestRate,
            @RequestParam(required = false) Integer termMonths) {
        BigDecimal payoff = calculationService.calculatePayoffAmount(approvedAmount, interestRate, termMonths);
        Map<String, Object> result = new HashMap<>();
        result.put("approvedAmount", approvedAmount);
        result.put("interestRate", interestRate);
        result.put("termMonths", termMonths != null ? termMonths : 60);
        result.put("payoffAmount", payoff);
        return ResponseEntity.ok(result);
    }

    // --- Late Fee Endpoint ---

    @PostMapping("/late-fee")
    public ResponseEntity<Payment> assessLateFee(
            @RequestParam Long loanId,
            @RequestParam int daysPastDue,
            @RequestParam BigDecimal currentBalance) {
        Payment fee = lateFeeService.assessLateFee(loanId, daysPastDue, currentBalance);
        return ResponseEntity.status(HttpStatus.CREATED).body(fee);
    }

    // --- Event Audit Endpoint ---

    @GetMapping("/events")
    public ResponseEntity<List<PaymentEvent>> getAllEvents() {
        return ResponseEntity.ok(eventStore.getAll());
    }

    @GetMapping("/events/{loanId}")
    public ResponseEntity<List<PaymentEvent>> getEventsByLoan(@PathVariable Long loanId) {
        return ResponseEntity.ok(eventStore.getByLoanId(loanId));
    }
}
