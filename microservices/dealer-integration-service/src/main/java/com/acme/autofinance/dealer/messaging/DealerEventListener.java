package com.acme.autofinance.dealer.messaging;

import com.acme.autofinance.dealer.domain.DealLoanProjection;
import com.acme.autofinance.dealer.domain.ProcessedEvent;
import com.acme.autofinance.dealer.repository.DealLoanProjectionRepository;
import com.acme.autofinance.dealer.repository.ProcessedEventRepository;
import com.acme.autofinance.dealer.service.DealerService;
import com.acme.autofinance.events.CreditDecisionMade;
import com.acme.autofinance.events.EventTopics;
import com.acme.autofinance.events.LoanApplicationSubmitted;
import com.acme.autofinance.events.LoanFunded;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Consumes loan-origination events to maintain the local deal/loan-status projection
 * and to advance dealer-owned deal status asynchronously — never calling another
 * service. Every handler is idempotent: a duplicate delivery of the same event id
 * is recorded once and otherwise ignored.
 */
@Component
@KafkaListener(topics = EventTopics.LOAN_ORIGINATION,
        containerFactory = "kafkaListenerContainerFactory")
public class DealerEventListener {

    private final DealLoanProjectionRepository dealLoanProjectionRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final DealerService dealerService;

    public DealerEventListener(DealLoanProjectionRepository dealLoanProjectionRepository,
                               ProcessedEventRepository processedEventRepository,
                               DealerService dealerService) {
        this.dealLoanProjectionRepository = dealLoanProjectionRepository;
        this.processedEventRepository = processedEventRepository;
        this.dealerService = dealerService;
    }

    @KafkaHandler
    @Transactional
    public void on(LoanApplicationSubmitted event) {
        if (alreadyProcessed(event.getEventId())) {
            return;
        }
        DealLoanProjection projection = dealLoanProjectionRepository.findById(event.getApplicationId())
                .orElseGet(DealLoanProjection::new);
        projection.setApplicationId(event.getApplicationId());
        projection.setApplicationNumber(event.getApplicationNumber());
        projection.setDealerId(event.getDealerId());
        projection.setVehicleVin(event.getVehicleVin());
        projection.setLoanStatus("SUBMITTED");
        dealLoanProjectionRepository.save(projection);

        dealerService.advanceOnApplicationSubmitted(event.getVehicleVin());
    }

    @KafkaHandler
    @Transactional
    public void on(CreditDecisionMade event) {
        if (alreadyProcessed(event.getEventId())) {
            return;
        }
        Optional<DealLoanProjection> projectionOpt =
                dealLoanProjectionRepository.findById(event.getApplicationId());
        if (!projectionOpt.isPresent()) {
            return;
        }
        DealLoanProjection projection = projectionOpt.get();
        projection.setLoanStatus(event.getDecision());
        projection.setApprovedAmount(event.getApprovedAmount());
        dealLoanProjectionRepository.save(projection);

        dealerService.advanceOnCreditDecision(
                projection.getVehicleVin(), event.getDecision(), event.getApprovedAmount());
    }

    @KafkaHandler
    @Transactional
    public void on(LoanFunded event) {
        if (alreadyProcessed(event.getEventId())) {
            return;
        }
        Optional<DealLoanProjection> projectionOpt =
                dealLoanProjectionRepository.findByApplicationNumber(event.getApplicationNumber());
        if (!projectionOpt.isPresent()) {
            return;
        }
        DealLoanProjection projection = projectionOpt.get();
        projection.setLoanId(event.getLoanId());
        projection.setLoanStatus("FUNDED");
        if (event.getApprovedAmount() != null) {
            projection.setApprovedAmount(event.getApprovedAmount());
        }
        dealLoanProjectionRepository.save(projection);

        dealerService.advanceOnLoanFunded(projection.getVehicleVin(), event.getLoanId());
    }

    /** Ignores unrelated event types that share the subscribed topic. */
    @KafkaHandler(isDefault = true)
    public void onOther(Object ignored) {
        // No-op: only loan-origination events relevant to dealer deals are handled.
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
