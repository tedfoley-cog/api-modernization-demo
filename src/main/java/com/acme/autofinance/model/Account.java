package com.acme.autofinance.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.math.BigDecimal;
import java.util.Date;

@Entity
@Table(name = "accounts")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_number", unique = true, nullable = false)
    private String accountNumber;

    @Column(name = "loan_id")
    private Long loanId;

    @Column(name = "customer_name")
    private String customerName;

    @Column(name = "customer_email")
    private String customerEmail;

    @Column(name = "customer_phone")
    private String customerPhone;

    @Column(name = "mailing_address")
    private String mailingAddress;

    @Column(name = "original_balance", precision = 12, scale = 2)
    private BigDecimal originalBalance;

    @Column(name = "current_balance", precision = 12, scale = 2)
    private BigDecimal currentBalance;

    @Column(name = "payoff_amount", precision = 12, scale = 2)
    private BigDecimal payoffAmount;

    @Column(name = "days_past_due")
    private Integer daysPastDue;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private AccountStatus status;

    @Temporal(TemporalType.DATE)
    @Column(name = "next_due_date")
    private Date nextDueDate;

    @Temporal(TemporalType.DATE)
    @Column(name = "maturity_date")
    private Date maturityDate;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "last_payment_date")
    private Date lastPaymentDate;

    public Account() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    public Long getLoanId() { return loanId; }
    public void setLoanId(Long loanId) { this.loanId = loanId; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }

    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }

    public String getMailingAddress() { return mailingAddress; }
    public void setMailingAddress(String mailingAddress) { this.mailingAddress = mailingAddress; }

    public BigDecimal getOriginalBalance() { return originalBalance; }
    public void setOriginalBalance(BigDecimal originalBalance) { this.originalBalance = originalBalance; }

    public BigDecimal getCurrentBalance() { return currentBalance; }
    public void setCurrentBalance(BigDecimal currentBalance) { this.currentBalance = currentBalance; }

    public BigDecimal getPayoffAmount() { return payoffAmount; }
    public void setPayoffAmount(BigDecimal payoffAmount) { this.payoffAmount = payoffAmount; }

    public Integer getDaysPastDue() { return daysPastDue; }
    public void setDaysPastDue(Integer daysPastDue) { this.daysPastDue = daysPastDue; }

    public AccountStatus getStatus() { return status; }
    public void setStatus(AccountStatus status) { this.status = status; }

    public Date getNextDueDate() { return nextDueDate; }
    public void setNextDueDate(Date nextDueDate) { this.nextDueDate = nextDueDate; }

    public Date getMaturityDate() { return maturityDate; }
    public void setMaturityDate(Date maturityDate) { this.maturityDate = maturityDate; }

    public Date getLastPaymentDate() { return lastPaymentDate; }
    public void setLastPaymentDate(Date lastPaymentDate) { this.lastPaymentDate = lastPaymentDate; }
}
