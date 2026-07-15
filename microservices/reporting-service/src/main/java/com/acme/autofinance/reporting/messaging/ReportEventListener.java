package com.acme.autofinance.reporting.messaging;

import com.acme.autofinance.events.AccountBalanceUpdated;
import com.acme.autofinance.events.AccountCreated;
import com.acme.autofinance.events.AccountDelinquent;
import com.acme.autofinance.events.CreditDecisionMade;
import com.acme.autofinance.events.DealPackageSubmitted;
import com.acme.autofinance.events.DealerSettlementCalculated;
import com.acme.autofinance.events.EventTopics;
import com.acme.autofinance.events.LateFeesAssessed;
import com.acme.autofinance.events.LoanApplicationSubmitted;
import com.acme.autofinance.events.LoanFunded;
import com.acme.autofinance.events.PaymentProcessed;
import com.acme.autofinance.events.PaymentReceived;
import com.acme.autofinance.reporting.domain.AccountReportProjection;
import com.acme.autofinance.reporting.domain.DealReportProjection;
import com.acme.autofinance.reporting.domain.DealerReportProjection;
import com.acme.autofinance.reporting.domain.LoanReportProjection;
import com.acme.autofinance.reporting.domain.PaymentReportProjection;
import com.acme.autofinance.reporting.domain.ProcessedEvent;
import com.acme.autofinance.reporting.repository.AccountReportRepository;
import com.acme.autofinance.reporting.repository.DealReportRepository;
import com.acme.autofinance.reporting.repository.DealerReportRepository;
import com.acme.autofinance.reporting.repository.LoanReportRepository;
import com.acme.autofinance.reporting.repository.PaymentReportRepository;
import com.acme.autofinance.reporting.repository.ProcessedEventRepository;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Component
@KafkaListener(topics = {
        EventTopics.LOAN_ORIGINATION,
        EventTopics.PAYMENT_PROCESSING,
        EventTopics.ACCOUNT_SERVICING,
        EventTopics.DEALER_INTEGRATION
}, containerFactory = "kafkaListenerContainerFactory")
public class ReportEventListener {

    private final LoanReportRepository loanRepository;
    private final AccountReportRepository accountRepository;
    private final PaymentReportRepository paymentRepository;
    private final DealerReportRepository dealerRepository;
    private final DealReportRepository dealRepository;
    private final ProcessedEventRepository processedEventRepository;

    public ReportEventListener(
            LoanReportRepository loanRepository,
            AccountReportRepository accountRepository,
            PaymentReportRepository paymentRepository,
            DealerReportRepository dealerRepository,
            DealReportRepository dealRepository,
            ProcessedEventRepository processedEventRepository) {
        this.loanRepository = loanRepository;
        this.accountRepository = accountRepository;
        this.paymentRepository = paymentRepository;
        this.dealerRepository = dealerRepository;
        this.dealRepository = dealRepository;
        this.processedEventRepository = processedEventRepository;
    }

    @KafkaHandler
    @Transactional
    public void on(LoanApplicationSubmitted event) {
        if (alreadyProcessed(event.getEventId())) return;
        LoanReportProjection loan = loan(event.getApplicationId());
        loan.setApplicationNumber(event.getApplicationNumber());
        loan.setDealerId(event.getDealerId());
        loan.setVehicleVin(event.getVehicleVin());
        loan.setRequestedAmount(event.getRequestedAmount());
        loanRepository.save(loan);
    }

    @KafkaHandler
    @Transactional
    public void on(CreditDecisionMade event) {
        if (alreadyProcessed(event.getEventId())) return;
        LoanReportProjection loan = loan(event.getApplicationId());
        loan.setCreditDecision(event.getDecision());
        loan.setApprovedAmount(event.getApprovedAmount());
        loanRepository.save(loan);
    }

    @KafkaHandler
    @Transactional
    public void on(LoanFunded event) {
        if (alreadyProcessed(event.getEventId())) return;
        LoanReportProjection loan = loan(event.getLoanId());
        loan.setApplicationNumber(event.getApplicationNumber());
        loan.setApprovedAmount(event.getApprovedAmount());
        loan.setFunded(true);
        loan.setFundingDate(event.getFundingDate());
        loanRepository.save(loan);
    }

    @KafkaHandler
    @Transactional
    public void on(PaymentReceived event) {
        if (alreadyProcessed(event.getEventId())) return;
        PaymentReportProjection payment = payment(event.getPaymentId());
        payment.setLoanId(event.getLoanId());
        payment.setPaymentAmount(event.getPaymentAmount());
        paymentRepository.save(payment);
    }

    @KafkaHandler
    @Transactional
    public void on(PaymentProcessed event) {
        if (alreadyProcessed(event.getEventId())) return;
        PaymentReportProjection payment = payment(event.getPaymentId());
        payment.setLoanId(event.getLoanId());
        payment.setStatus(event.getStatus());
        payment.setProcessedDate(event.getProcessedDate());
        payment.setNewBalance(event.getNewBalance());
        paymentRepository.save(payment);
    }

    @KafkaHandler
    @Transactional
    public void on(LateFeesAssessed event) {
        if (alreadyProcessed(event.getEventId())) return;
        LoanReportProjection loan = loan(event.getLoanId());
        BigDecimal existing = value(loan.getLateFeesAssessed());
        loan.setLateFeesAssessed(existing.add(value(event.getFeeAmount())));
        loanRepository.save(loan);
    }

    @KafkaHandler
    @Transactional
    public void on(AccountCreated event) {
        if (alreadyProcessed(event.getEventId())) return;
        AccountReportProjection account = account(event.getAccountId());
        account.setAccountNumber(event.getAccountNumber());
        account.setLoanId(event.getLoanId());
        account.setCustomerName(event.getCustomerName());
        account.setOriginalBalance(event.getOriginalBalance());
        if (account.getCurrentBalance() == null) {
            account.setCurrentBalance(event.getOriginalBalance());
        }
        accountRepository.save(account);
    }

    @KafkaHandler
    @Transactional
    public void on(AccountBalanceUpdated event) {
        if (alreadyProcessed(event.getEventId())) return;
        AccountReportProjection account = account(event.getAccountId());
        account.setCurrentBalance(event.getNewBalance());
        accountRepository.save(account);
    }

    @KafkaHandler
    @Transactional
    public void on(AccountDelinquent event) {
        if (alreadyProcessed(event.getEventId())) return;
        AccountReportProjection account = account(event.getAccountId());
        account.setLoanId(event.getLoanId());
        account.setDaysPastDue(event.getDaysPastDue());
        account.setCurrentBalance(event.getCurrentBalance());
        accountRepository.save(account);
    }

    @KafkaHandler
    @Transactional
    public void on(DealPackageSubmitted event) {
        if (alreadyProcessed(event.getEventId())) return;
        DealReportProjection deal = dealRepository.findById(event.getDealNumber())
                .orElseGet(() -> new DealReportProjection(event.getDealNumber()));
        deal.setDealerId(event.getDealerId());
        deal.setVehicleVin(event.getVehicleVin());
        deal.setSalePrice(event.getSalePrice());
        dealRepository.save(deal);
    }

    @KafkaHandler
    @Transactional
    public void on(DealerSettlementCalculated event) {
        if (alreadyProcessed(event.getEventId())) return;
        DealerReportProjection dealer = dealerRepository.findById(event.getDealerId())
                .orElseGet(() -> new DealerReportProjection(event.getDealerId()));
        dealer.setDealerCode(event.getDealerCode());
        dealer.setTotalReserves(event.getTotalReserves());
        dealer.setTotalHoldbacks(event.getTotalHoldbacks());
        dealer.setNetSettlement(event.getNetSettlement());
        dealerRepository.save(dealer);
    }

    @KafkaHandler(isDefault = true)
    public void onOther(Object ignored) {
    }

    private LoanReportProjection loan(Long id) {
        return loanRepository.findById(id).orElseGet(() -> new LoanReportProjection(id));
    }

    private AccountReportProjection account(Long id) {
        return accountRepository.findById(id).orElseGet(() -> new AccountReportProjection(id));
    }

    private PaymentReportProjection payment(Long id) {
        return paymentRepository.findById(id).orElseGet(() -> new PaymentReportProjection(id));
    }

    private boolean alreadyProcessed(String eventId) {
        if (eventId == null) return false;
        if (processedEventRepository.existsById(eventId)) return true;
        processedEventRepository.save(new ProcessedEvent(eventId));
        return false;
    }

    private static BigDecimal value(BigDecimal amount) {
        return amount != null ? amount : BigDecimal.ZERO;
    }
}
