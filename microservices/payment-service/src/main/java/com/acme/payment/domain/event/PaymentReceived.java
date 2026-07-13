package com.acme.payment.domain.event;

import java.math.BigDecimal;

/**
 * Published when a payment is successfully received and allocated.
 * Subscribers: Account Service (update balance), Reporting (update projections).
 */
public class PaymentReceived extends PaymentEvent {

    private final Long paymentId;
    private final BigDecimal totalAmount;
    private final BigDecimal principalAmount;
    private final BigDecimal interestAmount;
    private final BigDecimal feeAmount;
    private final String confirmationNumber;

    public PaymentReceived(Long loanId, Long paymentId, BigDecimal totalAmount,
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
