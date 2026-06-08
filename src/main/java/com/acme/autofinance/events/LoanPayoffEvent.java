package com.acme.autofinance.events;

import java.math.BigDecimal;

/**
 * Published by the Loan context when total completed payments satisfy the loan
 * and it transitions to {@code PAID_OFF} (in reaction to a
 * {@link PaymentCompletedEvent}).
 */
public class LoanPayoffEvent extends DomainEvent {

    private final Long loanId;
    private final BigDecimal totalPaid;

    public LoanPayoffEvent(Long loanId, BigDecimal totalPaid) {
        this.loanId = loanId;
        this.totalPaid = totalPaid;
    }

    public Long getLoanId() {
        return loanId;
    }

    public BigDecimal getTotalPaid() {
        return totalPaid;
    }
}
