package com.acme.payment.event;

import java.math.BigDecimal;

/**
 * Emitted once a payment amount has been split into fees, interest and principal
 * (allocation order: late fees &rarr; interest &rarr; principal). The loan domain can
 * consume this to update amortization without the payment service touching loan data.
 */
public final class PaymentAllocated extends DomainEvent {

    private final Long paymentId;
    private final Long loanId;
    private final BigDecimal feeAmount;
    private final BigDecimal interestAmount;
    private final BigDecimal principalAmount;

    public PaymentAllocated(Long paymentId, Long loanId, BigDecimal feeAmount,
                            BigDecimal interestAmount, BigDecimal principalAmount) {
        super(String.valueOf(paymentId));
        this.paymentId = paymentId;
        this.loanId = loanId;
        this.feeAmount = feeAmount;
        this.interestAmount = interestAmount;
        this.principalAmount = principalAmount;
    }

    @Override
    public String getEventType() {
        return "payment.allocated";
    }

    public Long getPaymentId() { return paymentId; }
    public Long getLoanId() { return loanId; }
    public BigDecimal getFeeAmount() { return feeAmount; }
    public BigDecimal getInterestAmount() { return interestAmount; }
    public BigDecimal getPrincipalAmount() { return principalAmount; }
}
