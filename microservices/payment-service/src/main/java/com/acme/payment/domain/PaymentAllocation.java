package com.acme.payment.domain;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Value object describing how a payment was split across fees, interest and principal.
 *
 * <p>In the monolith this breakdown was loose fields scattered on the {@code Payment}
 * row and mutated inline by {@code PaymentService.allocatePayment()}. Here it is an
 * immutable value object owned by the {@link Payment} aggregate.
 */
@Embeddable
public class PaymentAllocation implements Serializable {

    @Column(name = "fee_amount", precision = 12, scale = 2)
    private BigDecimal feeAmount;

    @Column(name = "interest_amount", precision = 12, scale = 2)
    private BigDecimal interestAmount;

    @Column(name = "principal_amount", precision = 12, scale = 2)
    private BigDecimal principalAmount;

    protected PaymentAllocation() {
    }

    public PaymentAllocation(BigDecimal feeAmount, BigDecimal interestAmount, BigDecimal principalAmount) {
        this.feeAmount = feeAmount;
        this.interestAmount = interestAmount;
        this.principalAmount = principalAmount;
    }

    public static PaymentAllocation empty() {
        return new PaymentAllocation(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
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
}
