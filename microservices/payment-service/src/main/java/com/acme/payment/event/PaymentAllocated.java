package com.acme.payment.event;

import java.math.BigDecimal;

/**
 * Emitted once a payment is broken down into principal/interest/fees.
 *
 * <p>Monolith equivalent: inline mutation in {@code PaymentService.allocatePayment()}.
 * Consumers: account-servicing (to amortize the balance).
 */
public final class PaymentAllocated extends DomainEvent {

    private final Long paymentId;
    private final Long loanId;
    private final BigDecimal principalAmount;
    private final BigDecimal interestAmount;
    private final BigDecimal feeAmount;

    public PaymentAllocated(Long paymentId, Long loanId, BigDecimal principalAmount,
                            BigDecimal interestAmount, BigDecimal feeAmount) {
        this.paymentId = paymentId;
        this.loanId = loanId;
        this.principalAmount = principalAmount;
        this.interestAmount = interestAmount;
        this.feeAmount = feeAmount;
    }

    @Override
    public String getEventType() {
        return "payment.allocated";
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
