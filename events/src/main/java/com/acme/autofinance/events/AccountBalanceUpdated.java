package com.acme.autofinance.events;

import java.math.BigDecimal;

/** Emitted when a balance changes after payment, adjustment, or payoff. */
public final class AccountBalanceUpdated extends DomainEvent {

    private final Long accountId;
    private final BigDecimal previousBalance;
    private final BigDecimal newBalance;
    private final BigDecimal changeAmount;
    private final String changeReason;

    public AccountBalanceUpdated(Long accountId, BigDecimal previousBalance, BigDecimal newBalance,
                                 BigDecimal changeAmount, String changeReason) {
        super(String.valueOf(accountId));
        this.accountId = accountId;
        this.previousBalance = previousBalance;
        this.newBalance = newBalance;
        this.changeAmount = changeAmount;
        this.changeReason = changeReason;
    }

    public Long getAccountId() {
        return accountId;
    }

    public BigDecimal getPreviousBalance() {
        return previousBalance;
    }

    public BigDecimal getNewBalance() {
        return newBalance;
    }

    public BigDecimal getChangeAmount() {
        return changeAmount;
    }

    public String getChangeReason() {
        return changeReason;
    }

    @Override
    public String eventType() {
        return "AccountBalanceUpdated";
    }

    @Override
    public String topic() {
        return EventTopics.ACCOUNT_SERVICING;
    }
}
