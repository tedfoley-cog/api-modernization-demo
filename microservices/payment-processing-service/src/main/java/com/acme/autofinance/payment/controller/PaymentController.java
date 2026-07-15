package com.acme.autofinance.payment.controller;

import com.acme.autofinance.payment.domain.Payment;
import com.acme.autofinance.payment.service.PaymentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * REST API for the payment-processing bounded context. Endpoint paths and
 * response shapes are preserved from the legacy monolith controller.
 */
@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public ResponseEntity<Payment> submitPayment(@RequestBody Payment payment) {
        if (payment.getLoanId() == null) {
            throw new IllegalArgumentException("Loan ID is required");
        }
        if (payment.getPaymentAmount() == null || payment.getPaymentAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amount must be positive");
        }
        Payment result = paymentService.submitPayment(payment);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @GetMapping("/loan/{loanId}")
    public ResponseEntity<List<Payment>> getPaymentHistory(@PathVariable Long loanId) {
        return ResponseEntity.ok(paymentService.getPaymentHistory(loanId));
    }

    @PostMapping("/batch")
    public ResponseEntity<Map<String, Object>> processBatch(@RequestBody List<Payment> payments) {
        return ResponseEntity.ok(paymentService.processBatchPayments(payments));
    }

    @GetMapping("/pending")
    public ResponseEntity<List<Payment>> getPendingPayments() {
        return ResponseEntity.ok(paymentService.getPendingPayments());
    }
}
