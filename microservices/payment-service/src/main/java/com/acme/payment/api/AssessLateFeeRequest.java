package com.acme.payment.api;

import com.acme.payment.command.AssessLateFeeCommand;

import java.math.BigDecimal;

/**
 * Inbound JSON body for assessing a late fee.
 */
public class AssessLateFeeRequest {

    private Long loanId;
    private int daysPastDue;
    private BigDecimal outstandingBalance;

    public AssessLateFeeCommand toCommand() {
        return new AssessLateFeeCommand(loanId, daysPastDue, outstandingBalance);
    }

    public Long getLoanId() { return loanId; }
    public void setLoanId(Long loanId) { this.loanId = loanId; }

    public int getDaysPastDue() { return daysPastDue; }
    public void setDaysPastDue(int daysPastDue) { this.daysPastDue = daysPastDue; }

    public BigDecimal getOutstandingBalance() { return outstandingBalance; }
    public void setOutstandingBalance(BigDecimal outstandingBalance) { this.outstandingBalance = outstandingBalance; }
}
