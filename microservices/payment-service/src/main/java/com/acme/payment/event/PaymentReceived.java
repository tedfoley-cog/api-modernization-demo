package com.acme.payment.event;

import java.math.BigDecimal;

/**
 * Published when a payment is submitted by a borrower.
 * Replaces the synchronous call in monolith PaymentService.submitPayment().
 */
public class PaymentReceived extends DomainEvent {

    private final Long paymentId;
    private final Long loanId;
    private final BigDecimal paymentAmount;
    private final String paymentMethod;
    private final String confirmationNumber;

    public PaymentReceived(Long paymentId, Long loanId, BigDecimal paymentAmount,
                           String paymentMethod, String confirmationNumber) {
        super("PaymentReceived");
        this.paymentId = paymentId;
        this.loanId = loanId;
        this.paymentAmount = paymentAmount;
        this.paymentMethod = paymentMethod;
        this.confirmationNumber = confirmationNumber;
    }

    public Long getPaymentId() { return paymentId; }
    public Long getLoanId() { return loanId; }
    public BigDecimal getPaymentAmount() { return paymentAmount; }
    public String getPaymentMethod() { return paymentMethod; }
    public String getConfirmationNumber() { return confirmationNumber; }
}
