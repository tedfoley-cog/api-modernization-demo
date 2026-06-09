package com.acme.payment.event;

import com.acme.payment.domain.PaymentMethod;

import java.math.BigDecimal;

/**
 * Emitted when a payment has been accepted into the system (status PENDING),
 * before allocation or processing. Downstream domains use this for audit and
 * to anticipate an incoming balance change.
 */
public final class PaymentReceived extends DomainEvent {

    private final Long paymentId;
    private final Long loanId;
    private final BigDecimal paymentAmount;
    private final PaymentMethod paymentMethod;
    private final String confirmationNumber;

    public PaymentReceived(Long paymentId, Long loanId, BigDecimal paymentAmount,
                           PaymentMethod paymentMethod, String confirmationNumber) {
        super(String.valueOf(paymentId));
        this.paymentId = paymentId;
        this.loanId = loanId;
        this.paymentAmount = paymentAmount;
        this.paymentMethod = paymentMethod;
        this.confirmationNumber = confirmationNumber;
    }

    @Override
    public String getEventType() {
        return "payment.received";
    }

    public Long getPaymentId() { return paymentId; }
    public Long getLoanId() { return loanId; }
    public BigDecimal getPaymentAmount() { return paymentAmount; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public String getConfirmationNumber() { return confirmationNumber; }
}
