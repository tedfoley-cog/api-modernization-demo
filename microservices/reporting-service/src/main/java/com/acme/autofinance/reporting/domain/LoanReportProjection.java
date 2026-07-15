package com.acme.autofinance.reporting.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "reporting_loans")
public class LoanReportProjection {

    @Id
    private Long id;

    @Column(name = "application_number", unique = true)
    private String applicationNumber;

    @Column(name = "dealer_id")
    private Long dealerId;

    @Column(name = "vehicle_vin")
    private String vehicleVin;

    @Column(name = "requested_amount", precision = 12, scale = 2)
    private BigDecimal requestedAmount;

    @Column(name = "approved_amount", precision = 12, scale = 2)
    private BigDecimal approvedAmount;

    @Column(name = "credit_decision")
    private String creditDecision;

    @Column(name = "funded")
    private boolean funded;

    @Column(name = "funding_date")
    private LocalDate fundingDate;

    @Column(name = "late_fees_assessed", precision = 12, scale = 2)
    private BigDecimal lateFeesAssessed = BigDecimal.ZERO;

    public LoanReportProjection() {}

    public LoanReportProjection(Long id) {
        this.id = id;
    }

    public Long getId() { return id; }
    public String getApplicationNumber() { return applicationNumber; }
    public void setApplicationNumber(String applicationNumber) { this.applicationNumber = applicationNumber; }
    public Long getDealerId() { return dealerId; }
    public void setDealerId(Long dealerId) { this.dealerId = dealerId; }
    public String getVehicleVin() { return vehicleVin; }
    public void setVehicleVin(String vehicleVin) { this.vehicleVin = vehicleVin; }
    public BigDecimal getRequestedAmount() { return requestedAmount; }
    public void setRequestedAmount(BigDecimal requestedAmount) { this.requestedAmount = requestedAmount; }
    public BigDecimal getApprovedAmount() { return approvedAmount; }
    public void setApprovedAmount(BigDecimal approvedAmount) { this.approvedAmount = approvedAmount; }
    public String getCreditDecision() { return creditDecision; }
    public void setCreditDecision(String creditDecision) { this.creditDecision = creditDecision; }
    public boolean isFunded() { return funded; }
    public void setFunded(boolean funded) { this.funded = funded; }
    public LocalDate getFundingDate() { return fundingDate; }
    public void setFundingDate(LocalDate fundingDate) { this.fundingDate = fundingDate; }
    public BigDecimal getLateFeesAssessed() { return lateFeesAssessed; }
    public void setLateFeesAssessed(BigDecimal lateFeesAssessed) { this.lateFeesAssessed = lateFeesAssessed; }
}
