package com.acme.payment.command;

import java.math.BigDecimal;

/**
 * CQRS command: encapsulates the intent to submit a payment.
 * Separates the write model from direct controller-to-service coupling.
 */
public class SubmitPaymentCommand {

    private final Long loanId;
    private final BigDecimal paymentAmount;
    private final String paymentMethod;
    private final String achRoutingNumber;
    private final String achAccountNumber;

    public SubmitPaymentCommand(Long loanId, BigDecimal paymentAmount, String paymentMethod,
                                String achRoutingNumber, String achAccountNumber) {
        this.loanId = loanId;
        this.paymentAmount = paymentAmount;
        this.paymentMethod = paymentMethod;
        this.achRoutingNumber = achRoutingNumber;
        this.achAccountNumber = achAccountNumber;
    }

    public Long getLoanId() { return loanId; }
    public BigDecimal getPaymentAmount() { return paymentAmount; }
    public String getPaymentMethod() { return paymentMethod; }
    public String getAchRoutingNumber() { return achRoutingNumber; }
    public String getAchAccountNumber() { return achAccountNumber; }
}
