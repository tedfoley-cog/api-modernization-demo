package com.acme.autofinance.events;

import java.math.BigDecimal;

/** Emitted when a dealer submits a deal package. */
public final class DealPackageSubmitted extends DomainEvent {

    private final String dealNumber;
    private final Long dealerId;
    private final String vehicleVin;
    private final BigDecimal salePrice;
    private final BigDecimal downPayment;
    private final BigDecimal tradeInValue;

    public DealPackageSubmitted(String dealNumber, Long dealerId, String vehicleVin,
                                BigDecimal salePrice, BigDecimal downPayment, BigDecimal tradeInValue) {
        super(dealNumber);
        this.dealNumber = dealNumber;
        this.dealerId = dealerId;
        this.vehicleVin = vehicleVin;
        this.salePrice = salePrice;
        this.downPayment = downPayment;
        this.tradeInValue = tradeInValue;
    }

    public String getDealNumber() {
        return dealNumber;
    }

    public Long getDealerId() {
        return dealerId;
    }

    public String getVehicleVin() {
        return vehicleVin;
    }

    public BigDecimal getSalePrice() {
        return salePrice;
    }

    public BigDecimal getDownPayment() {
        return downPayment;
    }

    public BigDecimal getTradeInValue() {
        return tradeInValue;
    }

    @Override
    public String eventType() {
        return "DealPackageSubmitted";
    }

    @Override
    public String topic() {
        return EventTopics.DEALER_INTEGRATION;
    }
}
