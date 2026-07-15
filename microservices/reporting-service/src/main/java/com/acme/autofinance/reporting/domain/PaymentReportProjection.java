package com.acme.autofinance.reporting.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "reporting_payments")
public class PaymentReportProjection {

    @Id
    private Long id;

    @Column(name = "loan_id")
    private Long loanId;

    @Column(name = "payment_amount", precision = 12, scale = 2)
    private BigDecimal paymentAmount;

    @Column(name = "status")
    private String status;

    @Column(name = "processed_date")
    private LocalDate processedDate;

    @Column(name = "new_balance", precision = 12, scale = 2)
    private BigDecimal newBalance;

    public PaymentReportProjection() {}

    public PaymentReportProjection(Long id) {
        this.id = id;
    }

    public Long getId() { return id; }
    public Long getLoanId() { return loanId; }
    public void setLoanId(Long loanId) { this.loanId = loanId; }
    public BigDecimal getPaymentAmount() { return paymentAmount; }
    public void setPaymentAmount(BigDecimal paymentAmount) { this.paymentAmount = paymentAmount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDate getProcessedDate() { return processedDate; }
    public void setProcessedDate(LocalDate processedDate) { this.processedDate = processedDate; }
    public BigDecimal getNewBalance() { return newBalance; }
    public void setNewBalance(BigDecimal newBalance) { this.newBalance = newBalance; }
}
