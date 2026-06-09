package com.acme.payment.command;

import java.math.BigDecimal;

/**
 * Command to assess a late fee against a loan.
 *
 * <p>{@code outstandingBalance} is supplied by the caller (account domain context) so the
 * percentage-based fee for accounts &gt;30 days past due can be computed without a
 * cross-domain lookup.
 */
public final class AssessLateFeeCommand {

    private final Long loanId;
    private final int daysPastDue;
    private final BigDecimal outstandingBalance;

    public AssessLateFeeCommand(Long loanId, int daysPastDue, BigDecimal outstandingBalance) {
        this.loanId = loanId;
        this.daysPastDue = daysPastDue;
        this.outstandingBalance = outstandingBalance;
    }

    public Long getLoanId() { return loanId; }
    public int getDaysPastDue() { return daysPastDue; }
    public BigDecimal getOutstandingBalance() { return outstandingBalance; }
}
