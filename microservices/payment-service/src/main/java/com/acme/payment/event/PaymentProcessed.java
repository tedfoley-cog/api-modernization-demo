package com.acme.payment.event;

import com.acme.payment.domain.PaymentStatus;

import java.math.BigDecimal;

/**
 * Emitted when a payment finishes processing (e.g. ACH settlement).
 *
 * <p>This event is the decoupling point that replaces the monolith's synchronous
 * {@code updateAccountBalance} / {@code updateLoanAfterPayment} calls: it carries the
 * principal applied so the account domain can reduce its balance, and the completed-to-date
 * total so the loan domain can decide whether the loan is paid off and the account closed.
 */
public final class PaymentProcessed extends DomainEvent {

    private final Long paymentId;
    private final Long loanId;
    private final PaymentStatus status;
    private final BigDecimal principalApplied;
    private final BigDecimal totalCompletedToDate;

    public PaymentProcessed(Long paymentId, Long loanId, PaymentStatus status,
                            BigDecimal principalApplied, BigDecimal totalCompletedToDate) {
        super(String.valueOf(paymentId));
        this.paymentId = paymentId;
        this.loanId = loanId;
        this.status = status;
        this.principalApplied = principalApplied;
        this.totalCompletedToDate = totalCompletedToDate;
    }

    @Override
    public String getEventType() {
        return "payment.processed";
    }

    public Long getPaymentId() { return paymentId; }
    public Long getLoanId() { return loanId; }
    public PaymentStatus getStatus() { return status; }
    public BigDecimal getPrincipalApplied() { return principalApplied; }
    public BigDecimal getTotalCompletedToDate() { return totalCompletedToDate; }
}
