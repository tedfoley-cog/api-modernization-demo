package com.acme.autofinance.payment.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.math.BigDecimal;

/**
 * Local, read-only projection of the loan facts this service needs to allocate
 * payments. Populated asynchronously from {@code LoanFunded} events; this module
 * never reads the loan-origination database or calls its service.
 */
@Entity
@Table(name = "loan_projection")
public class LoanProjection {

    @Id
    @Column(name = "loan_id")
    private Long loanId;

    @Column(name = "application_number")
    private String applicationNumber;

    @Column(name = "approved_amount", precision = 12, scale = 2)
    private BigDecimal approvedAmount;

    @Column(name = "interest_rate", precision = 6, scale = 3)
    private BigDecimal interestRate;

    @Column(name = "term_months")
    private Integer termMonths;

    public LoanProjection() {}

    public LoanProjection(Long loanId, String applicationNumber, BigDecimal approvedAmount,
                          BigDecimal interestRate, Integer termMonths) {
        this.loanId = loanId;
        this.applicationNumber = applicationNumber;
        this.approvedAmount = approvedAmount;
        this.interestRate = interestRate;
        this.termMonths = termMonths;
    }

    public Long getLoanId() { return loanId; }
    public void setLoanId(Long loanId) { this.loanId = loanId; }

    public String getApplicationNumber() { return applicationNumber; }
    public void setApplicationNumber(String applicationNumber) { this.applicationNumber = applicationNumber; }

    public BigDecimal getApprovedAmount() { return approvedAmount; }
    public void setApprovedAmount(BigDecimal approvedAmount) { this.approvedAmount = approvedAmount; }

    public BigDecimal getInterestRate() { return interestRate; }
    public void setInterestRate(BigDecimal interestRate) { this.interestRate = interestRate; }

    public Integer getTermMonths() { return termMonths; }
    public void setTermMonths(Integer termMonths) { this.termMonths = termMonths; }
}
