package com.acme.payment.command;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

/**
 * Write-side command to assess a late fee against a delinquent account.
 *
 * <p>The current balance is supplied at the boundary; the fee schedule lives inside the
 * payment domain.
 */
public class AssessLateFeeCommand {

    @NotNull
    private Long loanId;

    @NotNull
    @PositiveOrZero
    private Integer daysPastDue;

    @NotNull
    private BigDecimal currentBalance;

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

    public BigDecimal getCurrentBalance() {
        return currentBalance;
    }

    public void setCurrentBalance(BigDecimal currentBalance) {
        this.currentBalance = currentBalance;
    }
}
