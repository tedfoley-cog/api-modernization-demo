package com.acme.autofinance.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.Instant;

/** Emitted when a new account is created after loan approval. */
public final class AccountCreated extends DomainEvent {

    private final Long accountId;
    private final String accountNumber;
    private final Long loanId;
    private final String customerName;
    private final BigDecimal originalBalance;

    public AccountCreated(Long accountId, String accountNumber, Long loanId,
                          String customerName, BigDecimal originalBalance) {
        super(String.valueOf(accountId));
        this.accountId = accountId;
        this.accountNumber = accountNumber;
        this.loanId = loanId;
        this.customerName = customerName;
        this.originalBalance = originalBalance;
    }

    @JsonCreator
    public AccountCreated(
            @JsonProperty("eventId") String eventId,
            @JsonProperty("occurredAt") Instant occurredAt,
            @JsonProperty("aggregateId") String aggregateId,
            @JsonProperty("accountId") Long accountId,
            @JsonProperty("accountNumber") String accountNumber,
            @JsonProperty("loanId") Long loanId,
            @JsonProperty("customerName") String customerName,
            @JsonProperty("originalBalance") BigDecimal originalBalance) {
        super(eventId, occurredAt, aggregateId);
        this.accountId = accountId;
        this.accountNumber = accountNumber;
        this.loanId = loanId;
        this.customerName = customerName;
        this.originalBalance = originalBalance;
    }

    public Long getAccountId() {
        return accountId;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public Long getLoanId() {
        return loanId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public BigDecimal getOriginalBalance() {
        return originalBalance;
    }

    @Override
    public String eventType() {
        return "AccountCreated";
    }

    @Override
    public String topic() {
        return EventTopics.ACCOUNT_SERVICING;
    }
}
