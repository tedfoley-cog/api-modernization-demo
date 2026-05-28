package com.acme.payment.event;

import java.math.BigDecimal;

/**
 * Published when a payment is broken down into principal, interest, and fee portions.
 * Replaces inline allocation logic in monolith PaymentService.allocatePayment().
 */
public class PaymentAllocated extends DomainEvent {

    private final Long paymentId;
    private final Long loanId;
    private final BigDecimal principalAmount;
    private final BigDecimal interestAmount;
    private final BigDecimal feeAmount;

    public PaymentAllocated(Long paymentId, Long loanId, BigDecimal principalAmount,
                            BigDecimal interestAmount, BigDecimal feeAmount) {
        super("PaymentAllocated");
        this.paymentId = paymentId;
        this.loanId = loanId;
        this.principalAmount = principalAmount;
        this.interestAmount = interestAmount;
        this.feeAmount = feeAmount;
    }

    public Long getPaymentId() { return paymentId; }
    public Long getLoanId() { return loanId; }
    public BigDecimal getPrincipalAmount() { return principalAmount; }
    public BigDecimal getInterestAmount() { return interestAmount; }
    public BigDecimal getFeeAmount() { return feeAmount; }
}
