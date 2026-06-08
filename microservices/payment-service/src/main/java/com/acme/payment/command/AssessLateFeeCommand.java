package com.acme.payment.command;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

/**
 * Write-side request to assess a late fee against a loan. The outstanding
 * balance needed for the percentage tier is passed in, not fetched.
 */
public class AssessLateFeeCommand {

    @NotNull
    private Long loanId;

    @NotNull
    @PositiveOrZero
    private Integer daysPastDue;

    private BigDecimal outstandingBalance;

    public AssessLateFeeCommand() {
    }

    public Long getLoanId() {
        return loanId;
    }

    public void setLoanId(Long loanId) {
        this.loanId = loanId;
    }

    public Integer getDaysPastDue() {
        return daysPastDue;
    }

    public void setDaysPastDue(Integer daysPastDue) {
        this.daysPastDue = daysPastDue;
    }

    public BigDecimal getOutstandingBalance() {
        return outstandingBalance;
    }

    public void setOutstandingBalance(BigDecimal outstandingBalance) {
        this.outstandingBalance = outstandingBalance;
    }
}
