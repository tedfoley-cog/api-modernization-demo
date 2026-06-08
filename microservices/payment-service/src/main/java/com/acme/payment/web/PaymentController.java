package com.acme.payment.web;

import com.acme.payment.command.AssessLateFeeCommand;
import com.acme.payment.command.PaymentCommandService;
import com.acme.payment.command.SubmitPaymentCommand;
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
 * HTTP API for the payment context. Commands and queries are routed to separate services
 * (CQRS); the controller holds no business logic, unlike the monolith's controller which
 * validated and orchestrated inline.
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
        PaymentView view = PaymentView.from(commandService.submitPayment(command));
        return ResponseEntity.status(HttpStatus.CREATED).body(view);
    }

    @PostMapping("/late-fees")
    public ResponseEntity<PaymentView> assessLateFee(@Valid @RequestBody AssessLateFeeCommand command) {
        PaymentView view = PaymentView.from(commandService.assessLateFee(command));
        return ResponseEntity.status(HttpStatus.CREATED).body(view);
    }

    @GetMapping("/loan/{loanId}")
    public ResponseEntity<List<PaymentView>> getPaymentHistory(@PathVariable Long loanId) {
        return ResponseEntity.ok(queryService.getPaymentHistory(loanId));
    }

    @GetMapping("/pending")
    public ResponseEntity<List<PaymentView>> getPendingPayments() {
        return ResponseEntity.ok(queryService.getPendingPayments());
    }
}
