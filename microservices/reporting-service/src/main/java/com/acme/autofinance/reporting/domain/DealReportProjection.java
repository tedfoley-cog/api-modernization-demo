package com.acme.autofinance.reporting.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "reporting_deals")
public class DealReportProjection {

    @Id
    @Column(name = "deal_number")
    private String dealNumber;

    @Column(name = "dealer_id")
    private Long dealerId;

    @Column(name = "vehicle_vin")
    private String vehicleVin;

    @Column(name = "sale_price", precision = 12, scale = 2)
    private BigDecimal salePrice;

    public DealReportProjection() {}

    public DealReportProjection(String dealNumber) {
        this.dealNumber = dealNumber;
    }

    public String getDealNumber() { return dealNumber; }
    public Long getDealerId() { return dealerId; }
    public void setDealerId(Long dealerId) { this.dealerId = dealerId; }
    public String getVehicleVin() { return vehicleVin; }
    public void setVehicleVin(String vehicleVin) { this.vehicleVin = vehicleVin; }
    public BigDecimal getSalePrice() { return salePrice; }
    public void setSalePrice(BigDecimal salePrice) { this.salePrice = salePrice; }
}
