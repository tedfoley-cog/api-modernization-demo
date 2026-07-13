package com.acme.payment.domain.event;

import java.math.BigDecimal;

/**
 * Published when a payment finishes processing (ACH cleared, check deposited, etc.).
 * Subscribers: Loan Service (check payoff status), Account Service (update DPD).
 */
public class PaymentCompleted extends PaymentEvent {

    private final Long paymentId;
    private final BigDecimal principalAmount;
    private final BigDecimal totalPaidToDate;
    private final String confirmationNumber;

    public PaymentCompleted(Long loanId, Long paymentId, BigDecimal principalAmount,
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
