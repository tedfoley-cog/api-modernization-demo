package com.acme.autofinance.event;

/**
 * Published when a payment fails processing (bad ACH routing, NSF, etc.).
 * Subscribers: Account Service (no balance update), Notification Service.
 */
public class PaymentFailedEvent extends PaymentEvent {

    private final Long paymentId;
    private final String reason;
    private final String confirmationNumber;

    public PaymentFailedEvent(Long loanId, Long paymentId, String reason, String confirmationNumber) {
        super("PaymentFailed", loanId);
        this.paymentId = paymentId;
        this.reason = reason;
        this.confirmationNumber = confirmationNumber;
    }

    public Long getPaymentId() { return paymentId; }
    public String getReason() { return reason; }
    public String getConfirmationNumber() { return confirmationNumber; }
}
