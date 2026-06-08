package com.acme.payment.command;

import com.acme.payment.domain.PaymentMethod;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.math.BigDecimal;

/**
 * Write-side request to submit a payment. Allocation inputs (outstanding
 * balance, monthly rate, outstanding fees) are carried on the command rather
 * than fetched from the loan/account domains — this is the decoupling boundary.
 */
public class SubmitPaymentCommand {

    @NotNull
    private Long loanId;

    @NotNull
    @Positive
    private BigDecimal paymentAmount;

    @NotNull
    private PaymentMethod paymentMethod;

    private String achRoutingNumber;
    private String achAccountNumber;

    // Allocation inputs supplied by the caller (no cross-domain repository access).
    private BigDecimal outstandingBalance;
    private BigDecimal monthlyInterestRate;
    private BigDecimal outstandingFees;

    public SubmitPaymentCommand() {
    }

    public Long getLoanId() {
        return loanId;
    }

    public void setLoanId(Long loanId) {
        this.loanId = loanId;
    }

    public BigDecimal getPaymentAmount() {
        return paymentAmount;
    }

    public void setPaymentAmount(BigDecimal paymentAmount) {
        this.paymentAmount = paymentAmount;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getAchRoutingNumber() {
        return achRoutingNumber;
    }

    public void setAchRoutingNumber(String achRoutingNumber) {
        this.achRoutingNumber = achRoutingNumber;
    }

    public String getAchAccountNumber() {
        return achAccountNumber;
    }

    public void setAchAccountNumber(String achAccountNumber) {
        this.achAccountNumber = achAccountNumber;
    }

    public BigDecimal getOutstandingBalance() {
        return outstandingBalance;
    }

    public void setOutstandingBalance(BigDecimal outstandingBalance) {
        this.outstandingBalance = outstandingBalance;
    }

    public BigDecimal getMonthlyInterestRate() {
        return monthlyInterestRate;
    }

    public void setMonthlyInterestRate(BigDecimal monthlyInterestRate) {
        this.monthlyInterestRate = monthlyInterestRate;
    }

    public BigDecimal getOutstandingFees() {
        return outstandingFees;
    }

    public void setOutstandingFees(BigDecimal outstandingFees) {
        this.outstandingFees = outstandingFees;
    }
}
