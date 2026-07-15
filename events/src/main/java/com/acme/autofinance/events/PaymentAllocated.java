package com.acme.autofinance.events;

import java.math.BigDecimal;

/** Emitted when a payment is broken down into principal/interest/fees. */
public final class PaymentAllocated extends DomainEvent {

    private final Long paymentId;
    private final Long loanId;
    private final BigDecimal principalAmount;
    private final BigDecimal interestAmount;
    private final BigDecimal feeAmount;

    public PaymentAllocated(Long paymentId, Long loanId, BigDecimal principalAmount,
                            BigDecimal interestAmount, BigDecimal feeAmount) {
        super(String.valueOf(paymentId));
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

    @Override
    public String eventType() {
        return "PaymentAllocated";
    }

    @Override
    public String topic() {
        return EventTopics.PAYMENT_PROCESSING;
    }
}
