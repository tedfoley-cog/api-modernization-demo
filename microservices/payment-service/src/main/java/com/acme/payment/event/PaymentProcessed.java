package com.acme.payment.event;

import com.acme.payment.domain.PaymentStatus;

import java.math.BigDecimal;

/**
 * Emitted when an ACH/EFT payment finishes processing (success or failure).
 *
 * <p>Monolith equivalent: {@code PaymentService.processAchPayment()}, which synchronously
 * mutated the account balance and loan status. Those side effects now belong to the
 * consumers: account-servicing, loan-origination, reporting.
 */
public final class PaymentProcessed extends DomainEvent {

    private final Long paymentId;
    private final Long loanId;
    private final PaymentStatus status;
    private final BigDecimal principalApplied;

    public PaymentProcessed(Long paymentId, Long loanId, PaymentStatus status, BigDecimal principalApplied) {
        this.paymentId = paymentId;
        this.loanId = loanId;
        this.status = status;
        this.principalApplied = principalApplied;
    }

    @Override
    public String getEventType() {
        return "payment.processed";
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public Long getLoanId() {
        return loanId;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public BigDecimal getPrincipalApplied() {
        return principalApplied;
    }
}
