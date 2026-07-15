package com.acme.autofinance.events;

/**
 * Stable Kafka topic names for the auto finance event-driven architecture.
 * One topic per bounded context; all events produced by a domain are published
 * to that domain's topic.
 */
public final class EventTopics {

    public static final String LOAN_ORIGINATION = "autofinance.loan-origination.events";
    public static final String PAYMENT_PROCESSING = "autofinance.payment-processing.events";
    public static final String ACCOUNT_SERVICING = "autofinance.account-servicing.events";
    public static final String DEALER_INTEGRATION = "autofinance.dealer-integration.events";

    private EventTopics() {
    }
}
