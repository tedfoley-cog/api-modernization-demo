package com.acme.autofinance.account.domain;

public enum AccountStatus {
    PENDING_ACTIVATION,
    CURRENT,
    DELINQUENT_30,
    DELINQUENT_60,
    DELINQUENT_90,
    CHARGE_OFF,
    PAID_IN_FULL,
    EARLY_TERMINATION
}
