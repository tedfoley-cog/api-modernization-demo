package com.acme.payment.event;

import com.acme.payment.domain.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Emitted when a payment finishes processing (e.g. ACH settlement). Downstream
 * loan/account services react to this instead of being called synchronously.
 * Field names form part of the published event contract.
 */
public final class PaymentProcessed extends DomainEvent {

    private final Long paymentId;
    private final Long loanId;
    private final Instant processedDate;
    private final PaymentStatus status;
    private final BigDecimal principalApplied;
    private final BigDecimal newBalance;

    public PaymentProcessed(Long paymentId,
                            Long loanId,
                            Instant processedDate,
                            PaymentStatus status,
                            BigDecimal principalApplied,
                            BigDecimal newBalance) {
        super("PaymentProcessed", 1);
        this.paymentId = paymentId;
        this.loanId = loanId;
        this.processedDate = processedDate;
        this.status = status;
        this.principalApplied = principalApplied;
        this.newBalance = newBalance;
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public Long getLoanId() {
        return loanId;
    }

    public Instant getProcessedDate() {
        return processedDate;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public BigDecimal getPrincipalApplied() {
        return principalApplied;
    }

    public BigDecimal getNewBalance() {
        return newBalance;
    }
}
