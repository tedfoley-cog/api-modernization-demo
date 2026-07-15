package com.acme.autofinance.dealer.domain;

/** Lifecycle of a dealer deal package within the dealer-integration context. */
public enum DealStatus {
    SUBMITTED,
    UNDER_REVIEW,
    APPROVED,
    FUNDED,
    SETTLED,
    REJECTED
}
