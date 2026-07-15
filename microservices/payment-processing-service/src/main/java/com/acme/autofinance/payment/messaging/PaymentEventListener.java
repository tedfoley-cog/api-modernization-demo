package com.acme.autofinance.payment.messaging;

import com.acme.autofinance.events.AccountBalanceUpdated;
import com.acme.autofinance.events.AccountCreated;
import com.acme.autofinance.events.AccountDelinquent;
import com.acme.autofinance.events.LoanFunded;
import com.acme.autofinance.payment.domain.AccountProjection;
import com.acme.autofinance.payment.domain.LoanProjection;
import com.acme.autofinance.payment.domain.ProcessedEvent;
import com.acme.autofinance.payment.repository.AccountProjectionRepository;
import com.acme.autofinance.payment.repository.LoanProjectionRepository;
import com.acme.autofinance.payment.repository.ProcessedEventRepository;
import com.acme.autofinance.payment.service.PaymentService;
import com.acme.autofinance.events.EventTopics;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Consumes loan-origination and account-servicing events to maintain the local
 * projections this service reads, and to assess late fees on delinquency — never
 * calling another service. Every handler is idempotent: a duplicate delivery of
 * the same event id is recorded once and otherwise ignored.
 */
@Component
@KafkaListener(topics = {EventTopics.LOAN_ORIGINATION, EventTopics.ACCOUNT_SERVICING},
        containerFactory = "kafkaListenerContainerFactory")
public class PaymentEventListener {

    private final LoanProjectionRepository loanProjectionRepository;
    private final AccountProjectionRepository accountProjectionRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final PaymentService paymentService;

    public PaymentEventListener(LoanProjectionRepository loanProjectionRepository,
                                AccountProjectionRepository accountProjectionRepository,
                                ProcessedEventRepository processedEventRepository,
                                PaymentService paymentService) {
        this.loanProjectionRepository = loanProjectionRepository;
        this.accountProjectionRepository = accountProjectionRepository;
        this.processedEventRepository = processedEventRepository;
        this.paymentService = paymentService;
    }

    @KafkaHandler
    @Transactional
    public void on(LoanFunded event) {
        if (alreadyProcessed(event.getEventId())) {
            return;
        }
        LoanProjection projection = loanProjectionRepository.findById(event.getLoanId())
                .orElseGet(LoanProjection::new);
        projection.setLoanId(event.getLoanId());
        projection.setApplicationNumber(event.getApplicationNumber());
        projection.setApprovedAmount(event.getApprovedAmount());
        projection.setInterestRate(event.getInterestRate());
        projection.setTermMonths(event.getTermMonths());
        loanProjectionRepository.save(projection);
    }

    @KafkaHandler
    @Transactional
    public void on(AccountCreated event) {
        if (alreadyProcessed(event.getEventId())) {
            return;
        }
        AccountProjection projection = accountProjectionRepository.findById(event.getAccountId())
                .orElseGet(AccountProjection::new);
        projection.setAccountId(event.getAccountId());
        projection.setLoanId(event.getLoanId());
        projection.setCurrentBalance(event.getOriginalBalance());
        if (projection.getDaysPastDue() == null) {
            projection.setDaysPastDue(0);
        }
        accountProjectionRepository.save(projection);
    }

    @KafkaHandler
    @Transactional
    public void on(AccountBalanceUpdated event) {
        if (alreadyProcessed(event.getEventId())) {
            return;
        }
        Optional<AccountProjection> accountOpt = accountProjectionRepository.findById(event.getAccountId());
        if (!accountOpt.isPresent()) {
            return;
        }
        AccountProjection projection = accountOpt.get();
        projection.setCurrentBalance(event.getNewBalance());
        accountProjectionRepository.save(projection);
    }

    @KafkaHandler
    @Transactional
    public void on(AccountDelinquent event) {
        if (alreadyProcessed(event.getEventId())) {
            return;
        }
        AccountProjection projection = accountProjectionRepository.findById(event.getAccountId())
                .orElseGet(AccountProjection::new);
        projection.setAccountId(event.getAccountId());
        projection.setLoanId(event.getLoanId());
        if (event.getCurrentBalance() != null) {
            projection.setCurrentBalance(event.getCurrentBalance());
        }
        projection.setDaysPastDue(event.getDaysPastDue());
        accountProjectionRepository.save(projection);

        int daysPastDue = event.getDaysPastDue() != null ? event.getDaysPastDue() : 0;
        paymentService.assessLateFee(event.getLoanId(), daysPastDue);
    }

    /** Ignores unrelated event types that share the subscribed topics. */
    @KafkaHandler(isDefault = true)
    public void onOther(Object ignored) {
        // No-op: only loan/account projection-relevant events are handled.
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
