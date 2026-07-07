package com.acme.autofinance.event;

import java.math.BigDecimal;

/**
 * Published when a payment is received and allocated.
 * Subscribers: Account Service (update balance), Reporting (update projections).
 */
public class PaymentReceivedEvent extends PaymentEvent {

    private final Long paymentId;
    private final BigDecimal totalAmount;
    private final BigDecimal principalAmount;
    private final BigDecimal interestAmount;
    private final BigDecimal feeAmount;
    private final String confirmationNumber;

    public PaymentReceivedEvent(Long loanId, Long paymentId, BigDecimal totalAmount,
                                BigDecimal principalAmount, BigDecimal interestAmount,
                                BigDecimal feeAmount, String confirmationNumber) {
        super("PaymentReceived", loanId);
        this.paymentId = paymentId;
        this.totalAmount = totalAmount;
        this.principalAmount = principalAmount;
        this.interestAmount = interestAmount;
        this.feeAmount = feeAmount;
        this.confirmationNumber = confirmationNumber;
    }

    public Long getPaymentId() { return paymentId; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public BigDecimal getPrincipalAmount() { return principalAmount; }
    public BigDecimal getInterestAmount() { return interestAmount; }
    public BigDecimal getFeeAmount() { return feeAmount; }
    public String getConfirmationNumber() { return confirmationNumber; }
}
