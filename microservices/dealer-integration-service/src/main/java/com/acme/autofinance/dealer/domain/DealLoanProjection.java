package com.acme.autofinance.dealer.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.math.BigDecimal;

/**
 * Local, read-only projection of the loan facts the dealer context needs to track
 * a deal's origination progress and to compute inventory financing. Populated
 * asynchronously from loan-origination events; this module never reads the
 * loan-origination database or calls its service.
 */
@Entity
@Table(name = "deal_loan_projection")
public class DealLoanProjection {

    @Id
    @Column(name = "application_id")
    private Long applicationId;

    @Column(name = "application_number")
    private String applicationNumber;

    @Column(name = "dealer_id")
    private Long dealerId;

    @Column(name = "vehicle_vin")
    private String vehicleVin;

    @Column(name = "loan_id")
    private Long loanId;

    @Column(name = "loan_status")
    private String loanStatus;

    @Column(name = "approved_amount", precision = 12, scale = 2)
    private BigDecimal approvedAmount;

    public DealLoanProjection() {}

    public DealLoanProjection(Long applicationId, String applicationNumber, Long dealerId,
                              String vehicleVin, String loanStatus, BigDecimal approvedAmount) {
        this.applicationId = applicationId;
        this.applicationNumber = applicationNumber;
        this.dealerId = dealerId;
        this.vehicleVin = vehicleVin;
        this.loanStatus = loanStatus;
        this.approvedAmount = approvedAmount;
    }

    public Long getApplicationId() { return applicationId; }
    public void setApplicationId(Long applicationId) { this.applicationId = applicationId; }

    public String getApplicationNumber() { return applicationNumber; }
    public void setApplicationNumber(String applicationNumber) { this.applicationNumber = applicationNumber; }

    public Long getDealerId() { return dealerId; }
    public void setDealerId(Long dealerId) { this.dealerId = dealerId; }

    public String getVehicleVin() { return vehicleVin; }
    public void setVehicleVin(String vehicleVin) { this.vehicleVin = vehicleVin; }

    public Long getLoanId() { return loanId; }
    public void setLoanId(Long loanId) { this.loanId = loanId; }

    public String getLoanStatus() { return loanStatus; }
    public void setLoanStatus(String loanStatus) { this.loanStatus = loanStatus; }

    public BigDecimal getApprovedAmount() { return approvedAmount; }
    public void setApprovedAmount(BigDecimal approvedAmount) { this.approvedAmount = approvedAmount; }
}
