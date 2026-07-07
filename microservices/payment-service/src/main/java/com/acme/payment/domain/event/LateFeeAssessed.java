package com.acme.payment.domain.event;

import java.math.BigDecimal;

/**
 * Published when a late fee is assessed against a loan.
 * Subscribers: Account Service (add fee to outstanding balance), Notification Service.
 */
public class LateFeeAssessed extends PaymentEvent {

    private final BigDecimal feeAmount;
    private final int daysPastDue;
    private final String confirmationNumber;

    public LateFeeAssessed(Long loanId, BigDecimal feeAmount, int daysPastDue, String confirmationNumber) {
        super("LateFeeAssessed", loanId);
        this.feeAmount = feeAmount;
        this.daysPastDue = daysPastDue;
        this.confirmationNumber = confirmationNumber;
    }

    public BigDecimal getFeeAmount() { return feeAmount; }
    public int getDaysPastDue() { return daysPastDue; }
    public String getConfirmationNumber() { return confirmationNumber; }
}
