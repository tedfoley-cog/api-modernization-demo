package com.acme.payment.domain;

import java.math.BigDecimal;

/**
 * Immutable value object holding the result of splitting a payment amount into
 * fees, interest and principal portions.
 */
public final class PaymentAllocation {

    private final BigDecimal feeAmount;
    private final BigDecimal interestAmount;
    private final BigDecimal principalAmount;

    public PaymentAllocation(BigDecimal feeAmount, BigDecimal interestAmount, BigDecimal principalAmount) {
        this.feeAmount = feeAmount;
        this.interestAmount = interestAmount;
        this.principalAmount = principalAmount;
    }

    public BigDecimal getFeeAmount() { return feeAmount; }

    public BigDecimal getInterestAmount() { return interestAmount; }

    public BigDecimal getPrincipalAmount() { return principalAmount; }

    @Override
    public String toString() {
        return "PaymentAllocation{fees=" + feeAmount
                + ", interest=" + interestAmount
                + ", principal=" + principalAmount + '}';
    }
}
