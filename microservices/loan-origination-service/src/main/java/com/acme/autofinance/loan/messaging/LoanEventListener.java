package com.acme.autofinance.loan.messaging;

import com.acme.autofinance.events.DealPackageSubmitted;
import com.acme.autofinance.events.EventTopics;
import com.acme.autofinance.events.PaymentProcessed;
import com.acme.autofinance.loan.domain.ProcessedEvent;
import com.acme.autofinance.loan.repository.ProcessedEventRepository;
import com.acme.autofinance.loan.service.LoanService;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consumes the events loan origination reacts to. {@link DealPackageSubmitted} is
 * the asynchronous command that originates a loan application from a dealer
 * submission. {@link PaymentProcessed} drives a local loan-lifecycle projection
 * only (marking a loan paid off), never mutating payment/account/dealer state.
 *
 * <p>Every handler is idempotent: a duplicate delivery of the same event id is
 * recorded once and otherwise ignored, so a dealer submission never creates a
 * second loan application.
 */
@Component
@KafkaListener(topics = {EventTopics.DEALER_INTEGRATION, EventTopics.PAYMENT_PROCESSING},
        containerFactory = "kafkaListenerContainerFactory")
public class LoanEventListener {

    private final LoanService loanService;
    private final ProcessedEventRepository processedEventRepository;

    public LoanEventListener(LoanService loanService,
                             ProcessedEventRepository processedEventRepository) {
        this.loanService = loanService;
        this.processedEventRepository = processedEventRepository;
    }

    /** A dealer deal-package submission asynchronously originates a loan application. */
    @KafkaHandler
    @Transactional
    public void on(DealPackageSubmitted event) {
        if (alreadyProcessed(event.getEventId())) {
            return;
        }
        loanService.createApplicationFromDealSubmission(
                event.getDealNumber(),
                event.getDealerId(),
                event.getVehicleVin(),
                event.getSalePrice(),
                event.getDownPayment(),
                event.getTradeInValue());
    }

    /** Local loan-lifecycle projection from a completed payment. */
    @KafkaHandler
    @Transactional
    public void on(PaymentProcessed event) {
        if (alreadyProcessed(event.getEventId())) {
            return;
        }
        loanService.projectPaymentProcessed(event.getLoanId(), event.getNewBalance());
    }

    /** Ignores unrelated event types that share the subscribed topics. */
    @KafkaHandler(isDefault = true)
    public void onOther(Object ignored) {
        // No-op: only dealer submissions and payment completions concern loan origination.
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
}
