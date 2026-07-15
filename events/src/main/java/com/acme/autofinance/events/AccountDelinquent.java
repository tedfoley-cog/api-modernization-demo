package com.acme.autofinance.events;

import java.math.BigDecimal;

/** Emitted when an account crosses a delinquency threshold. */
public final class AccountDelinquent extends DomainEvent {

    private final Long accountId;
    private final Long loanId;
    private final Integer daysPastDue;
    private final String delinquencyBucket;
    private final BigDecimal currentBalance;

    public AccountDelinquent(Long accountId, Long loanId, Integer daysPastDue,
                             String delinquencyBucket, BigDecimal currentBalance) {
        super(String.valueOf(accountId));
        this.accountId = accountId;
        this.loanId = loanId;
        this.daysPastDue = daysPastDue;
        this.delinquencyBucket = delinquencyBucket;
        this.currentBalance = currentBalance;
    }

    public Long getAccountId() {
        return accountId;
    }

    public Long getLoanId() {
        return loanId;
    }

    public Integer getDaysPastDue() {
        return daysPastDue;
    }

    public String getDelinquencyBucket() {
        return delinquencyBucket;
    }

    public BigDecimal getCurrentBalance() {
        return currentBalance;
    }

    @Override
    public String eventType() {
        return "AccountDelinquent";
    }

    @Override
    public String topic() {
        return EventTopics.ACCOUNT_SERVICING;
    }
}
