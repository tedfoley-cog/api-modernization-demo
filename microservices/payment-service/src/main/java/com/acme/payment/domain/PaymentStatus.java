package com.acme.payment.domain;

/**
 * Lifecycle status of a payment within the payment bounded context.
 */
public enum PaymentStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED,
    REVERSED,
    NSF
}
