package com.acme.autofinance.dealer.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.math.BigDecimal;

/** Dealer aggregate owned by the dealer-integration bounded context. */
@Entity
@Table(name = "dealers")
public class Dealer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "dealer_code", unique = true, nullable = false)
    private String dealerCode;

    @Column(name = "dealer_name", nullable = false)
    private String dealerName;

    @Column(name = "contact_name")
    private String contactName;

    @Column(name = "contact_email")
    private String contactEmail;

    @Column(name = "region")
    private String region;

    @Column(name = "reserve_rate", precision = 5, scale = 2)
    private BigDecimal reserveRate;

    @Column(name = "holdback_pct", precision = 5, scale = 2)
    private BigDecimal holdbackPct;

    @Column(name = "active")
    private Boolean active;

    @Column(name = "tier")
    private String tier;

    @Column(name = "ytd_volume")
    private Integer ytdVolume;

    public Dealer() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getDealerCode() { return dealerCode; }
    public void setDealerCode(String dealerCode) { this.dealerCode = dealerCode; }

    public String getDealerName() { return dealerName; }
    public void setDealerName(String dealerName) { this.dealerName = dealerName; }

    public String getContactName() { return contactName; }
    public void setContactName(String contactName) { this.contactName = contactName; }

    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    public BigDecimal getReserveRate() { return reserveRate; }
    public void setReserveRate(BigDecimal reserveRate) { this.reserveRate = reserveRate; }

    public BigDecimal getHoldbackPct() { return holdbackPct; }
    public void setHoldbackPct(BigDecimal holdbackPct) { this.holdbackPct = holdbackPct; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public String getTier() { return tier; }
    public void setTier(String tier) { this.tier = tier; }

    public Integer getYtdVolume() { return ytdVolume; }
    public void setYtdVolume(Integer ytdVolume) { this.ytdVolume = ytdVolume; }
}
