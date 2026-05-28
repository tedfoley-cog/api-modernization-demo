package com.acme.payment;

import com.acme.payment.command.AssessLateFeeCommand;
import com.acme.payment.command.PaymentCommandHandler;
import com.acme.payment.command.ProcessBatchCommand;
import com.acme.payment.command.SubmitPaymentCommand;
import com.acme.payment.model.Payment;
import com.acme.payment.query.PaymentQueryHandler;
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
import java.util.stream.Collectors;

/**
 * Payment microservice REST API.
 * Uses CQRS: POST operations go through the command handler (writes),
 * GET operations go through the query handler (reads).
 */
@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentCommandHandler commandHandler;
    private final PaymentQueryHandler queryHandler;

    public PaymentController(PaymentCommandHandler commandHandler, PaymentQueryHandler queryHandler) {
        this.commandHandler = commandHandler;
        this.queryHandler = queryHandler;
    }

    // ========== COMMANDS (Write Side) ==========

    @PostMapping
    public ResponseEntity<Payment> submitPayment(@RequestBody SubmitPaymentCommand command) {
        if (command.getLoanId() == null) {
            throw new RuntimeException("Loan ID is required");
        }
        if (command.getPaymentAmount() == null || command.getPaymentAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Payment amount must be positive");
        }
        Payment result = commandHandler.handle(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PostMapping("/batch")
    public ResponseEntity<Map<String, Object>> processBatch(@RequestBody List<SubmitPaymentCommand> commands) {
        Map<String, Object> result = commandHandler.handle(new ProcessBatchCommand(commands));
        return ResponseEntity.ok(result);
    }

    @PostMapping("/late-fee")
    public ResponseEntity<Payment> assessLateFee(@RequestBody AssessLateFeeCommand command) {
        Payment result = commandHandler.handle(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    // ========== QUERIES (Read Side) ==========

    @GetMapping("/{id}")
    public ResponseEntity<Payment> getPayment(@PathVariable Long id) {
        return ResponseEntity.ok(queryHandler.getPaymentById(id));
    }

    @GetMapping("/loan/{loanId}")
    public ResponseEntity<List<Payment>> getPaymentHistory(@PathVariable Long loanId) {
        return ResponseEntity.ok(queryHandler.getPaymentHistory(loanId));
    }

    @GetMapping("/pending")
    public ResponseEntity<List<Payment>> getPendingPayments() {
        return ResponseEntity.ok(queryHandler.getPendingPayments());
    }

    @GetMapping("/loan/{loanId}/total")
    public ResponseEntity<BigDecimal> getTotalPaid(@PathVariable Long loanId) {
        return ResponseEntity.ok(queryHandler.getTotalPaidForLoan(loanId));
    }
}
