package com.acme.autofinance.events;

/**
 * Published by the Loan context (during end-of-day processing) to request that
 * the Payment context assess a late fee for a delinquent loan. Inverts the old
 * synchronous {@code LoanService -> PaymentService.assessLateFee()} call.
 */
public class LateFeeRequiredEvent extends DomainEvent {

    private final Long loanId;
    private final int daysPastDue;

    public LateFeeRequiredEvent(Long loanId, int daysPastDue) {
        this.loanId = loanId;
        this.daysPastDue = daysPastDue;
    }

    public Long getLoanId() {
        return loanId;
    }

    public int getDaysPastDue() {
        return daysPastDue;
    }
}
