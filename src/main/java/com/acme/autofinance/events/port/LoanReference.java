package com.acme.autofinance.events.port;

import java.math.BigDecimal;

/**
 * Read-only snapshot of the loan/account data the Payment context needs to
 * allocate a payment. This is the anti-corruption read model: the Payment
 * context never touches the {@code LoanApplication} or {@code Account}
 * aggregates directly.
 */
public class LoanReference {

    private final Long loanId;
    private final BigDecimal interestRate;
    private final BigDecimal approvedAmount;
    private final BigDecimal currentBalance;

    public LoanReference(Long loanId, BigDecimal interestRate, BigDecimal approvedAmount, BigDecimal currentBalance) {
        this.loanId = loanId;
        this.interestRate = interestRate;
        this.approvedAmount = approvedAmount;
        this.currentBalance = currentBalance;
    }

    public Long getLoanId() {
        return loanId;
    }

    public BigDecimal getInterestRate() {
        return interestRate;
    }

    public BigDecimal getApprovedAmount() {
        return approvedAmount;
    }

    /** Current outstanding balance from the Account context, may be {@code null}. */
    public BigDecimal getCurrentBalance() {
        return currentBalance;
    }
}
