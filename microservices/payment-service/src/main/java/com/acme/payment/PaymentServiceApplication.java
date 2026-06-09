package com.acme.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Standalone entry point for the payment processing microservice.
 *
 * <p>This service owns the Payment bounded context. It does not depend on the
 * monolith's {@code com.acme.autofinance.*} classes and does not share a database
 * with the loan or account domains. Cross-domain side effects (updating an account
 * balance, marking a loan paid off, closing an account) are no longer performed
 * synchronously here — instead they are expressed as published domain events that
 * other services subscribe to.
 */
@SpringBootApplication
public class PaymentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
    }
}
