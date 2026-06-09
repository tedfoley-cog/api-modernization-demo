package com.acme.payment.api;

import com.acme.payment.command.SubmitPaymentCommand;
import com.acme.payment.domain.PaymentMethod;

import java.math.BigDecimal;

/**
 * Inbound JSON body for submitting a payment. Includes the loan/account context the
 * caller supplies in place of the monolith's synchronous cross-domain lookups.
 */
public class SubmitPaymentRequest {

    private Long loanId;
    private BigDecimal paymentAmount;
    private PaymentMethod paymentMethod;
    private String achRoutingNumber;
    private String achAccountNumber;
    private BigDecimal outstandingBalance;
    private BigDecimal annualInterestRate;

    public SubmitPaymentCommand toCommand() {
        return new SubmitPaymentCommand(loanId, paymentAmount, paymentMethod,
                achRoutingNumber, achAccountNumber, outstandingBalance, annualInterestRate);
    }

    public Long getLoanId() { return loanId; }
    public void setLoanId(Long loanId) { this.loanId = loanId; }

    public BigDecimal getPaymentAmount() { return paymentAmount; }
    public void setPaymentAmount(BigDecimal paymentAmount) { this.paymentAmount = paymentAmount; }

    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getAchRoutingNumber() { return achRoutingNumber; }
    public void setAchRoutingNumber(String achRoutingNumber) { this.achRoutingNumber = achRoutingNumber; }

    public String getAchAccountNumber() { return achAccountNumber; }
    public void setAchAccountNumber(String achAccountNumber) { this.achAccountNumber = achAccountNumber; }

    public BigDecimal getOutstandingBalance() { return outstandingBalance; }
    public void setOutstandingBalance(BigDecimal outstandingBalance) { this.outstandingBalance = outstandingBalance; }

    public BigDecimal getAnnualInterestRate() { return annualInterestRate; }
    public void setAnnualInterestRate(BigDecimal annualInterestRate) { this.annualInterestRate = annualInterestRate; }
}
