package com.acme.payment.domain;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Immutable value type describing how a payment amount is split across
 * outstanding fees, interest and principal.
 */
public final class PaymentAllocation {

    private final BigDecimal feeAmount;
    private final BigDecimal interestAmount;
    private final BigDecimal principalAmount;

    public PaymentAllocation(BigDecimal feeAmount, BigDecimal interestAmount, BigDecimal principalAmount) {
        this.feeAmount = feeAmount == null ? BigDecimal.ZERO : feeAmount;
        this.interestAmount = interestAmount == null ? BigDecimal.ZERO : interestAmount;
        this.principalAmount = principalAmount == null ? BigDecimal.ZERO : principalAmount;
    }

    public BigDecimal getFeeAmount() {
        return feeAmount;
    }

    public BigDecimal getInterestAmount() {
        return interestAmount;
    }

    public BigDecimal getPrincipalAmount() {
        return principalAmount;
    }

    public BigDecimal total() {
        return feeAmount.add(interestAmount).add(principalAmount);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PaymentAllocation that = (PaymentAllocation) o;
        return feeAmount.compareTo(that.feeAmount) == 0
                && interestAmount.compareTo(that.interestAmount) == 0
                && principalAmount.compareTo(that.principalAmount) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(feeAmount.stripTrailingZeros(),
                interestAmount.stripTrailingZeros(),
                principalAmount.stripTrailingZeros());
    }

    @Override
    public String toString() {
        return "PaymentAllocation{fee=" + feeAmount
                + ", interest=" + interestAmount
                + ", principal=" + principalAmount + '}';
    }
}
