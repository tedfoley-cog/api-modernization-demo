package com.acme.payment.command;

import com.acme.payment.domain.PaymentMethod;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.math.BigDecimal;

/**
 * Write-side command to submit a payment.
 *
 * <p>The loan context required for allocation ({@code outstandingBalance},
 * {@code annualInterestRate}, {@code outstandingFees}) is supplied at the boundary — the
 * payment service never reaches into the loan or account databases the way the monolith did.
 */
public class SubmitPaymentCommand {

    @NotNull
    private Long loanId;

    @NotNull
    @Positive
    private BigDecimal paymentAmount;

    private PaymentMethod paymentMethod;

    private String achRoutingNumber;

    /** Outstanding principal balance, provided by the loan/account context. */
    private BigDecimal outstandingBalance;

    /** Annual interest rate (percent), provided by the loan context. */
    private BigDecimal annualInterestRate;

    /** Outstanding late fees to be cleared first, provided by the account context. */
    private BigDecimal outstandingFees;

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

    public BigDecimal getOutstandingBalance() {
        return outstandingBalance;
    }

    public void setOutstandingBalance(BigDecimal outstandingBalance) {
        this.outstandingBalance = outstandingBalance;
    }

    public BigDecimal getAnnualInterestRate() {
        return annualInterestRate;
    }

    public void setAnnualInterestRate(BigDecimal annualInterestRate) {
        this.annualInterestRate = annualInterestRate;
    }

    public BigDecimal getOutstandingFees() {
        return outstandingFees;
    }

    public void setOutstandingFees(BigDecimal outstandingFees) {
        this.outstandingFees = outstandingFees;
    }
}
