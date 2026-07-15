package com.acme.autofinance.account.messaging;

import com.acme.autofinance.account.domain.ProcessedEvent;
import com.acme.autofinance.account.repository.ProcessedEventRepository;
import com.acme.autofinance.account.service.AccountService;
import com.acme.autofinance.events.EventTopics;
import com.acme.autofinance.events.LoanFunded;
import com.acme.autofinance.events.PaymentAllocated;
import com.acme.autofinance.events.PaymentProcessed;
import com.acme.autofinance.events.PaymentReceived;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

/**
 * Consumes loan-origination and payment-processing events to drive account state:
 * a funded loan creates and activates the account here, and payment events update
 * account-owned balances and fees. This module never calls another service. Every
 * handler is idempotent: a duplicate delivery of the same event id is recorded
 * once and otherwise ignored, so balances and account creation never double-apply.
 */
@Component
@KafkaListener(topics = {EventTopics.LOAN_ORIGINATION, EventTopics.PAYMENT_PROCESSING},
        containerFactory = "kafkaListenerContainerFactory")
public class AccountEventListener {

    private final AccountService accountService;
    private final ProcessedEventRepository processedEventRepository;

    public AccountEventListener(AccountService accountService,
                                ProcessedEventRepository processedEventRepository) {
        this.accountService = accountService;
        this.processedEventRepository = processedEventRepository;
    }

    /** A funded loan creates (and activates) the account in this context. */
    @KafkaHandler
    @Transactional
    public void on(LoanFunded event) {
        if (alreadyProcessed(event.getEventId())) {
            return;
        }
        accountService.createAccountForLoan(
                event.getLoanId(),
                null, // customer name is not in the authoritative LoanFunded payload
                event.getApprovedAmount(),
                event.getInterestRate(),
                event.getTermMonths());
    }

    @KafkaHandler
    @Transactional
    public void on(PaymentReceived event) {
        if (alreadyProcessed(event.getEventId())) {
            return;
        }
        accountService.recordPaymentReceived(event.getLoanId(), toDate(event.getOccurredAt()));
    }

    @KafkaHandler
    @Transactional
    public void on(PaymentProcessed event) {
        if (alreadyProcessed(event.getEventId())) {
            return;
        }
        accountService.applyProcessedPayment(event.getLoanId(), event.getPrincipalApplied());
    }

    @KafkaHandler
    @Transactional
    public void on(PaymentAllocated event) {
        if (alreadyProcessed(event.getEventId())) {
            return;
        }
        accountService.applyFeeAllocation(event.getLoanId(), event.getFeeAmount());
    }

    /** Ignores unrelated event types that share the subscribed topics. */
    @KafkaHandler(isDefault = true)
    public void onOther(Object ignored) {
        // No-op: only loan-funding and payment events drive account state.
    }

    private boolean alreadyProcessed(String eventId) {
        if (eventId == null) {
            return false;
        }
        if (processedEventRepository.existsById(eventId)) {
            return true;
        }
        processedEventRepository.save(new ProcessedEvent(eventId));
        return false;
    }

    private static Date toDate(java.time.Instant instant) {
        return instant != null ? Date.from(instant) : new Date();
    }
}
