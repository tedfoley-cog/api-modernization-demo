package com.acme.payment.api;

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

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST endpoints for the payment service. Mirrors the monolith's payment API surface,
 * delegating writes to {@link PaymentCommandService} and reads to {@link PaymentQueryService}
 * (CQRS).
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
    public ResponseEntity<PaymentView> submitPayment(@RequestBody SubmitPaymentRequest request) {
        Payment result = commandService.submitPayment(request.toCommand());
        return ResponseEntity.status(HttpStatus.CREATED).body(PaymentView.from(result));
    }

    @GetMapping("/loan/{loanId}")
    public ResponseEntity<List<PaymentView>> getPaymentHistory(@PathVariable Long loanId) {
        return ResponseEntity.ok(queryService.getPaymentHistory(loanId));
    }

    @PostMapping("/batch")
    public ResponseEntity<BatchResult> processBatch(@RequestBody List<SubmitPaymentRequest> requests) {
        List<SubmitPaymentCommand> commands = requests.stream()
                .map(SubmitPaymentRequest::toCommand)
                .collect(Collectors.toList());
        return ResponseEntity.ok(commandService.processBatch(new ProcessBatchCommand(commands)));
    }

    @GetMapping("/pending")
    public ResponseEntity<List<PaymentView>> getPendingPayments() {
        return ResponseEntity.ok(queryService.getPendingPayments());
    }

    @PostMapping("/late-fee")
    public ResponseEntity<PaymentView> assessLateFee(@RequestBody AssessLateFeeRequest request) {
        Payment result = commandService.assessLateFee(request.toCommand());
        return ResponseEntity.status(HttpStatus.CREATED).body(PaymentView.from(result));
    }
}
