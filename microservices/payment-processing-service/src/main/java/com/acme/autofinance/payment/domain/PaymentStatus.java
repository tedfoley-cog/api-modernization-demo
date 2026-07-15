package com.acme.autofinance.payment.domain;

public enum PaymentStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED,
    REVERSED,
    NSF
}
