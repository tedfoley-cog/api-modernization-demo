package com.acme.autofinance.dealer.domain;

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

/**
 * Deal package aggregate owned by the dealer-integration context. The {@code loanId}
 * is a plain correlation column populated asynchronously from loan events; this
 * context holds no loan entity and never reads the loan-origination database.
 */
@Entity
@Table(name = "deal_packages")
public class DealPackage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "deal_number", unique = true, nullable = false)
    private String dealNumber;

    @Column(name = "dealer_id", nullable = false)
    private Long dealerId;

    @Column(name = "loan_id")
    private Long loanId;

    @Column(name = "vehicle_vin")
    private String vehicleVin;

    @Column(name = "sale_price", precision = 12, scale = 2)
    private BigDecimal salePrice;

    @Column(name = "down_payment", precision = 10, scale = 2)
    private BigDecimal downPayment;

    @Column(name = "trade_in_value", precision = 10, scale = 2)
    private BigDecimal tradeInValue;

    @Column(name = "dealer_reserve", precision = 10, scale = 2)
    private BigDecimal dealerReserve;

    @Column(name = "holdback_amount", precision = 10, scale = 2)
    private BigDecimal holdbackAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private DealStatus status;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "submission_date")
    private Date submissionDate;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "settlement_date")
    private Date settlementDate;

    public DealPackage() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getDealNumber() { return dealNumber; }
    public void setDealNumber(String dealNumber) { this.dealNumber = dealNumber; }

    public Long getDealerId() { return dealerId; }
    public void setDealerId(Long dealerId) { this.dealerId = dealerId; }

    public Long getLoanId() { return loanId; }
    public void setLoanId(Long loanId) { this.loanId = loanId; }

    public String getVehicleVin() { return vehicleVin; }
    public void setVehicleVin(String vehicleVin) { this.vehicleVin = vehicleVin; }

    public BigDecimal getSalePrice() { return salePrice; }
    public void setSalePrice(BigDecimal salePrice) { this.salePrice = salePrice; }

    public BigDecimal getDownPayment() { return downPayment; }
    public void setDownPayment(BigDecimal downPayment) { this.downPayment = downPayment; }

    public BigDecimal getTradeInValue() { return tradeInValue; }
    public void setTradeInValue(BigDecimal tradeInValue) { this.tradeInValue = tradeInValue; }

    public BigDecimal getDealerReserve() { return dealerReserve; }
    public void setDealerReserve(BigDecimal dealerReserve) { this.dealerReserve = dealerReserve; }

    public BigDecimal getHoldbackAmount() { return holdbackAmount; }
    public void setHoldbackAmount(BigDecimal holdbackAmount) { this.holdbackAmount = holdbackAmount; }

    public DealStatus getStatus() { return status; }
    public void setStatus(DealStatus status) { this.status = status; }

    public Date getSubmissionDate() { return submissionDate; }
    public void setSubmissionDate(Date submissionDate) { this.submissionDate = submissionDate; }

    public Date getSettlementDate() { return settlementDate; }
    public void setSettlementDate(Date settlementDate) { this.settlementDate = settlementDate; }
}
