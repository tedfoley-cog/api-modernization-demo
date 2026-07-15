package com.acme.autofinance.events;

import java.math.BigDecimal;

/** Emitted when settlement amounts are computed for a dealer. */
public final class DealerSettlementCalculated extends DomainEvent {

    private final Long dealerId;
    private final String dealerCode;
    private final BigDecimal totalReserves;
    private final BigDecimal totalHoldbacks;
    private final BigDecimal netSettlement;

    public DealerSettlementCalculated(Long dealerId, String dealerCode, BigDecimal totalReserves,
                                      BigDecimal totalHoldbacks, BigDecimal netSettlement) {
        super(String.valueOf(dealerId));
        this.dealerId = dealerId;
        this.dealerCode = dealerCode;
        this.totalReserves = totalReserves;
        this.totalHoldbacks = totalHoldbacks;
        this.netSettlement = netSettlement;
    }

    public Long getDealerId() {
        return dealerId;
    }

    public String getDealerCode() {
        return dealerCode;
    }

    public BigDecimal getTotalReserves() {
        return totalReserves;
    }

    public BigDecimal getTotalHoldbacks() {
        return totalHoldbacks;
    }

    public BigDecimal getNetSettlement() {
        return netSettlement;
    }

    @Override
    public String eventType() {
        return "DealerSettlementCalculated";
    }

    @Override
    public String topic() {
        return EventTopics.DEALER_INTEGRATION;
    }
}
