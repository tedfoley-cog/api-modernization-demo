package com.acme.payment.domain;

/**
 * Supported payment instruments. Mirrors the legacy monolith's payment methods
 * but lives entirely within the payment bounded context.
 */
public enum PaymentMethod {
    ACH,
    EFT,
    CHECK,
    WIRE,
    DEALER_REMITTANCE
}
