package com.acme.payment.web;

import com.acme.payment.command.BatchResult;
import com.acme.payment.command.PaymentCommandService;
import com.acme.payment.command.ProcessBatchCommand;
import com.acme.payment.command.SubmitPaymentCommand;
import com.acme.payment.domain.Payment;
import com.acme.payment.query.PaymentQueryService;
import com.acme.payment.query.PaymentView;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

/**
 * Thin REST adapter. Mirrors the monolith's {@code /api/payments} surface but
 * delegates writes to the command service and reads to the query service.
 */
@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentCommandService commandService;
    private final PaymentQueryService queryService;

    public PaymentController(PaymentCommandService commandService, PaymentQueryService queryService) {
        this.commandService = commandService;
        this.queryService = queryService;
    }

    @PostMapping
    public ResponseEntity<PaymentView> submitPayment(@Valid @RequestBody SubmitPaymentCommand command) {
        Payment saved = commandService.submit(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(PaymentView.from(saved));
    }

    @GetMapping("/loan/{loanId}")
    public ResponseEntity<List<PaymentView>> getPaymentHistory(@PathVariable Long loanId) {
        return ResponseEntity.ok(queryService.historyForLoan(loanId));
    }

    @PostMapping("/batch")
    public ResponseEntity<BatchResult> processBatch(@Valid @RequestBody ProcessBatchCommand command) {
        return ResponseEntity.ok(commandService.processBatch(command));
    }

    @GetMapping("/pending")
    public ResponseEntity<List<PaymentView>> getPendingPayments() {
        return ResponseEntity.ok(queryService.pendingPayments());
    }
}
