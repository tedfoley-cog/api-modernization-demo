package com.acme.payment.command;

import java.math.BigDecimal;

/**
 * CQRS command: encapsulates the intent to assess a late fee.
 * Replaces the synchronous call from LoanService to PaymentService.
 */
public class AssessLateFeeCommand {

    private final Long loanId;
    private final int daysPastDue;
    private final BigDecimal currentBalance;

    public AssessLateFeeCommand(Long loanId, int daysPastDue, BigDecimal currentBalance) {
        this.loanId = loanId;
        this.daysPastDue = daysPastDue;
        this.currentBalance = currentBalance;
    }

    public Long getLoanId() { return loanId; }
    public int getDaysPastDue() { return daysPastDue; }
    public BigDecimal getCurrentBalance() { return currentBalance; }
}
