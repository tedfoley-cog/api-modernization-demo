package com.acme.autofinance.reporting.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "reporting_accounts")
public class AccountReportProjection {

    @Id
    private Long id;

    @Column(name = "account_number")
    private String accountNumber;

    @Column(name = "loan_id")
    private Long loanId;

    @Column(name = "customer_name")
    private String customerName;

    @Column(name = "original_balance", precision = 12, scale = 2)
    private BigDecimal originalBalance;

    @Column(name = "current_balance", precision = 12, scale = 2)
    private BigDecimal currentBalance;

    @Column(name = "days_past_due")
    private Integer daysPastDue = 0;

    public AccountReportProjection() {}

    public AccountReportProjection(Long id) {
        this.id = id;
    }

    public Long getId() { return id; }
    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
    public Long getLoanId() { return loanId; }
    public void setLoanId(Long loanId) { this.loanId = loanId; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public BigDecimal getOriginalBalance() { return originalBalance; }
    public void setOriginalBalance(BigDecimal originalBalance) { this.originalBalance = originalBalance; }
    public BigDecimal getCurrentBalance() { return currentBalance; }
    public void setCurrentBalance(BigDecimal currentBalance) { this.currentBalance = currentBalance; }
    public Integer getDaysPastDue() { return daysPastDue; }
    public void setDaysPastDue(Integer daysPastDue) { this.daysPastDue = daysPastDue; }
}
