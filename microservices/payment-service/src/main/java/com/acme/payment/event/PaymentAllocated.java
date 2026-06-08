package com.acme.payment.event;

import java.math.BigDecimal;

/**
 * Emitted once a received payment has been split across principal, interest and
 * fees. Field names form part of the published event contract.
 */
public final class PaymentAllocated extends DomainEvent {

    private final Long paymentId;
    private final Long loanId;
    private final BigDecimal principalAmount;
    private final BigDecimal interestAmount;
    private final BigDecimal feeAmount;

    public PaymentAllocated(Long paymentId,
                            Long loanId,
                            BigDecimal principalAmount,
                            BigDecimal interestAmount,
                            BigDecimal feeAmount) {
        super("PaymentAllocated", 1);
        this.paymentId = paymentId;
        this.loanId = loanId;
        this.principalAmount = principalAmount;
        this.interestAmount = interestAmount;
        this.feeAmount = feeAmount;
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public Long getLoanId() {
        return loanId;
    }

    public BigDecimal getPrincipalAmount() {
        return principalAmount;
    }

    public BigDecimal getInterestAmount() {
        return interestAmount;
    }

    public BigDecimal getFeeAmount() {
        return feeAmount;
    }
}
