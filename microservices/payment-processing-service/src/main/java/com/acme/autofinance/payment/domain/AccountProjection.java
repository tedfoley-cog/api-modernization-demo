package com.acme.autofinance.payment.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Local, read-only projection of the account balance facts this service needs to
 * allocate payments and assess late fees. Populated asynchronously from
 * {@code AccountCreated}, {@code AccountBalanceUpdated}, and
 * {@code AccountDelinquent} events. Keyed by account id and correlated to a loan.
 */
@Entity
@Table(name = "account_projection")
public class AccountProjection {

    @Id
    @Column(name = "account_id")
    private Long accountId;

    @Column(name = "loan_id")
    private Long loanId;

    @Column(name = "current_balance", precision = 12, scale = 2)
    private BigDecimal currentBalance;

    @Column(name = "days_past_due")
    private Integer daysPastDue;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "last_payment_date")
    private Date lastPaymentDate;

    public AccountProjection() {}

    public AccountProjection(Long accountId, Long loanId, BigDecimal currentBalance) {
        this.accountId = accountId;
        this.loanId = loanId;
        this.currentBalance = currentBalance;
        this.daysPastDue = 0;
    }

    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }

    public Long getLoanId() { return loanId; }
    public void setLoanId(Long loanId) { this.loanId = loanId; }

    public BigDecimal getCurrentBalance() { return currentBalance; }
    public void setCurrentBalance(BigDecimal currentBalance) { this.currentBalance = currentBalance; }

    public Integer getDaysPastDue() { return daysPastDue; }
    public void setDaysPastDue(Integer daysPastDue) { this.daysPastDue = daysPastDue; }

    public Date getLastPaymentDate() { return lastPaymentDate; }
    public void setLastPaymentDate(Date lastPaymentDate) { this.lastPaymentDate = lastPaymentDate; }
}
