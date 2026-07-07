package com.acme.autofinance.event;

import java.math.BigDecimal;

/**
 * Published when a payment finishes processing.
 * Subscribers: Account Service (update balance, reset DPD), Loan Service (check payoff).
 */
public class PaymentCompletedEvent extends PaymentEvent {

    private final Long paymentId;
    private final BigDecimal principalAmount;
    private final BigDecimal totalPaidToDate;
    private final String confirmationNumber;

    public PaymentCompletedEvent(Long loanId, Long paymentId, BigDecimal principalAmount,
                                 BigDecimal totalPaidToDate, String confirmationNumber) {
        super("PaymentCompleted", loanId);
        this.paymentId = paymentId;
        this.principalAmount = principalAmount;
        this.totalPaidToDate = totalPaidToDate;
        this.confirmationNumber = confirmationNumber;
    }

    public Long getPaymentId() { return paymentId; }
    public BigDecimal getPrincipalAmount() { return principalAmount; }
    public BigDecimal getTotalPaidToDate() { return totalPaidToDate; }
    public String getConfirmationNumber() { return confirmationNumber; }
}
