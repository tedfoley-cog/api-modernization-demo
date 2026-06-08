package com.acme.payment.event;

import com.acme.payment.domain.PaymentMethod;

import java.math.BigDecimal;

/**
 * Emitted when a payment is accepted into the system (before downstream
 * processing). Field names form part of the published event contract.
 */
public final class PaymentReceived extends DomainEvent {

    private final Long paymentId;
    private final Long loanId;
    private final BigDecimal paymentAmount;
    private final PaymentMethod paymentMethod;
    private final String confirmationNumber;

    public PaymentReceived(Long paymentId,
                           Long loanId,
                           BigDecimal paymentAmount,
                           PaymentMethod paymentMethod,
                           String confirmationNumber) {
        super("PaymentReceived", 1);
        this.paymentId = paymentId;
        this.loanId = loanId;
        this.paymentAmount = paymentAmount;
        this.paymentMethod = paymentMethod;
        this.confirmationNumber = confirmationNumber;
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public Long getLoanId() {
        return loanId;
    }

    public BigDecimal getPaymentAmount() {
        return paymentAmount;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public String getConfirmationNumber() {
        return confirmationNumber;
    }
}
