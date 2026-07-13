package com.acme.payment.domain.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.math.BigDecimal;
import java.util.Date;

/**
 * CQRS read model: materialized view of payment totals per loan.
 * Updated by event handlers whenever a payment is processed.
 */
@Entity
@Table(name = "payment_summary")
public class PaymentSummary {

    @Id
    @Column(name = "loan_id")
    private Long loanId;

    @Column(name = "total_paid", precision = 12, scale = 2)
    private BigDecimal totalPaid;

    @Column(name = "total_principal_paid", precision = 12, scale = 2)
    private BigDecimal totalPrincipalPaid;

    @Column(name = "total_interest_paid", precision = 12, scale = 2)
    private BigDecimal totalInterestPaid;

    @Column(name = "total_fees_paid", precision = 12, scale = 2)
    private BigDecimal totalFeesPaid;

    @Column(name = "outstanding_late_fees", precision = 12, scale = 2)
    private BigDecimal outstandingLateFees;

    @Column(name = "payment_count")
    private Integer paymentCount;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "last_payment_date")
    private Date lastPaymentDate;

    @Column(name = "last_payment_amount", precision = 10, scale = 2)
    private BigDecimal lastPaymentAmount;

    public PaymentSummary() {}

    public Long getLoanId() { return loanId; }
    public void setLoanId(Long loanId) { this.loanId = loanId; }

    public BigDecimal getTotalPaid() { return totalPaid; }
    public void setTotalPaid(BigDecimal totalPaid) { this.totalPaid = totalPaid; }

    public BigDecimal getTotalPrincipalPaid() { return totalPrincipalPaid; }
    public void setTotalPrincipalPaid(BigDecimal totalPrincipalPaid) { this.totalPrincipalPaid = totalPrincipalPaid; }

    public BigDecimal getTotalInterestPaid() { return totalInterestPaid; }
    public void setTotalInterestPaid(BigDecimal totalInterestPaid) { this.totalInterestPaid = totalInterestPaid; }

    public BigDecimal getTotalFeesPaid() { return totalFeesPaid; }
    public void setTotalFeesPaid(BigDecimal totalFeesPaid) { this.totalFeesPaid = totalFeesPaid; }

    public BigDecimal getOutstandingLateFees() { return outstandingLateFees; }
    public void setOutstandingLateFees(BigDecimal outstandingLateFees) { this.outstandingLateFees = outstandingLateFees; }

    public Integer getPaymentCount() { return paymentCount; }
    public void setPaymentCount(Integer paymentCount) { this.paymentCount = paymentCount; }

    public Date getLastPaymentDate() { return lastPaymentDate; }
    public void setLastPaymentDate(Date lastPaymentDate) { this.lastPaymentDate = lastPaymentDate; }

    public BigDecimal getLastPaymentAmount() { return lastPaymentAmount; }
    public void setLastPaymentAmount(BigDecimal lastPaymentAmount) { this.lastPaymentAmount = lastPaymentAmount; }
}
