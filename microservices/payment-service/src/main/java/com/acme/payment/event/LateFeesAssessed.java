package com.acme.payment.event;

import java.math.BigDecimal;

/**
 * Emitted when a late fee is charged to a delinquent account.
 *
 * <p>Monolith equivalent: {@code PaymentService.assessLateFee()}, which was called
 * synchronously from {@code LoanService.runEndOfDayProcessing()}. Consumers:
 * account-servicing, reporting.
 */
public final class LateFeesAssessed extends DomainEvent {

    private final Long paymentId;
    private final Long loanId;
    private final BigDecimal feeAmount;
    private final int daysPastDue;

    public LateFeesAssessed(Long paymentId, Long loanId, BigDecimal feeAmount, int daysPastDue) {
        this.paymentId = paymentId;
        this.loanId = loanId;
        this.feeAmount = feeAmount;
        this.daysPastDue = daysPastDue;
    }

    @Override
    public String getEventType() {
        return "payment.late_fees_assessed";
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public Long getLoanId() {
        return loanId;
    }

    public BigDecimal getFeeAmount() {
        return feeAmount;
    }

    public int getDaysPastDue() {
        return daysPastDue;
    }
}
