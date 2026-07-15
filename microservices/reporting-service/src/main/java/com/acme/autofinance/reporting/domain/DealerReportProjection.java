package com.acme.autofinance.reporting.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "reporting_dealers")
public class DealerReportProjection {

    @Id
    private Long id;

    @Column(name = "dealer_code")
    private String dealerCode;

    @Column(name = "total_reserves", precision = 12, scale = 2)
    private BigDecimal totalReserves;

    @Column(name = "total_holdbacks", precision = 12, scale = 2)
    private BigDecimal totalHoldbacks;

    @Column(name = "net_settlement", precision = 12, scale = 2)
    private BigDecimal netSettlement;

    public DealerReportProjection() {}

    public DealerReportProjection(Long id) {
        this.id = id;
    }

    public Long getId() { return id; }
    public String getDealerCode() { return dealerCode; }
    public void setDealerCode(String dealerCode) { this.dealerCode = dealerCode; }
    public BigDecimal getTotalReserves() { return totalReserves; }
    public void setTotalReserves(BigDecimal totalReserves) { this.totalReserves = totalReserves; }
    public BigDecimal getTotalHoldbacks() { return totalHoldbacks; }
    public void setTotalHoldbacks(BigDecimal totalHoldbacks) { this.totalHoldbacks = totalHoldbacks; }
    public BigDecimal getNetSettlement() { return netSettlement; }
    public void setNetSettlement(BigDecimal netSettlement) { this.netSettlement = netSettlement; }
}
