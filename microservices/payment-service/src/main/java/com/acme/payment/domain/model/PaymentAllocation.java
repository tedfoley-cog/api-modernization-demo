package com.acme.payment.domain.model;

import java.math.BigDecimal;

/**
 * Value object representing how a payment is allocated across
 * principal, interest, and fees.
 */
public class PaymentAllocation {

    private final BigDecimal principalAmount;
    private final BigDecimal interestAmount;
    private final BigDecimal feeAmount;

    public PaymentAllocation(BigDecimal principalAmount, BigDecimal interestAmount, BigDecimal feeAmount) {
        this.principalAmount = principalAmount;
        this.interestAmount = interestAmount;
        this.feeAmount = feeAmount;
    }

    public BigDecimal getPrincipalAmount() { return principalAmount; }
    public BigDecimal getInterestAmount() { return interestAmount; }
    public BigDecimal getFeeAmount() { return feeAmount; }

    public BigDecimal getTotal() {
        return principalAmount.add(interestAmount).add(feeAmount);
    }
}
