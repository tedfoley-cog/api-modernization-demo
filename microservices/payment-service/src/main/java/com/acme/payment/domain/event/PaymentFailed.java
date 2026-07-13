package com.acme.payment.domain.event;

/**
 * Published when a payment fails processing (bad ACH routing, NSF, etc.).
 * Subscribers: Account Service (no balance update), Notification Service.
 */
public class PaymentFailed extends PaymentEvent {

    private final Long paymentId;
    private final String reason;
    private final String confirmationNumber;

    public PaymentFailed(Long loanId, Long paymentId, String reason, String confirmationNumber) {
        super("PaymentFailed", loanId);
        this.paymentId = paymentId;
        this.reason = reason;
        this.confirmationNumber = confirmationNumber;
    }

    public Long getPaymentId() { return paymentId; }
    public String getReason() { return reason; }
    public String getConfirmationNumber() { return confirmationNumber; }
}
