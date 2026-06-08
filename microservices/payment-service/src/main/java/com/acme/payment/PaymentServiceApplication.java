package com.acme.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Payment Processing microservice.
 *
 * <p>Extracted from the {@code LoanService}/{@code PaymentService} god-object in the
 * auto-finance monolith. Instead of synchronously reaching into the loan and account
 * domains, this service owns its data and communicates the outcome of every payment
 * via immutable domain events ({@code PaymentReceived}, {@code PaymentAllocated},
 * {@code PaymentProcessed}, {@code LateFeesAssessed}).
 */
@SpringBootApplication
public class PaymentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
    }
}
