package com.acme.payment.command;

import com.acme.payment.domain.PaymentMethod;

import java.math.BigDecimal;

/**
 * Command to submit a single payment.
 *
 * <p>Crucially, the cross-domain context the monolith used to fetch synchronously
 * ({@code outstandingBalance} from the account domain and {@code annualInterestRate}
 * from the loan domain) is provided as command input. The payment service therefore
 * never calls {@code loanRepository} or {@code accountRepository}.
 */
public final class SubmitPaymentCommand {

    private final Long loanId;
    private final BigDecimal paymentAmount;
    private final PaymentMethod paymentMethod;
    private final String achRoutingNumber;
    private final String achAccountNumber;
    private final BigDecimal outstandingBalance;
    private final BigDecimal annualInterestRate;

    public SubmitPaymentCommand(Long loanId, BigDecimal paymentAmount, PaymentMethod paymentMethod,
                                String achRoutingNumber, String achAccountNumber,
                                BigDecimal outstandingBalance, BigDecimal annualInterestRate) {
        this.loanId = loanId;
        this.paymentAmount = paymentAmount;
        this.paymentMethod = paymentMethod;
        this.achRoutingNumber = achRoutingNumber;
        this.achAccountNumber = achAccountNumber;
        this.outstandingBalance = outstandingBalance;
        this.annualInterestRate = annualInterestRate;
    }

    public Long getLoanId() { return loanId; }
    public BigDecimal getPaymentAmount() { return paymentAmount; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public String getAchRoutingNumber() { return achRoutingNumber; }
    public String getAchAccountNumber() { return achAccountNumber; }
    public BigDecimal getOutstandingBalance() { return outstandingBalance; }
    public BigDecimal getAnnualInterestRate() { return annualInterestRate; }
}
