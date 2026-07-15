package com.acme.autofinance.reporting.service;

import com.acme.autofinance.events.AccountBalanceUpdated;
import com.acme.autofinance.events.AccountCreated;
import com.acme.autofinance.events.AccountDelinquent;
import com.acme.autofinance.events.CreditDecisionMade;
import com.acme.autofinance.events.DealPackageSubmitted;
import com.acme.autofinance.events.DealerSettlementCalculated;
import com.acme.autofinance.events.LateFeesAssessed;
import com.acme.autofinance.events.LoanApplicationSubmitted;
import com.acme.autofinance.events.LoanFunded;
import com.acme.autofinance.events.PaymentProcessed;
import com.acme.autofinance.events.PaymentReceived;
import com.acme.autofinance.reporting.domain.AccountReportProjection;
import com.acme.autofinance.reporting.messaging.ReportEventListener;
import com.acme.autofinance.reporting.repository.AccountReportRepository;
import com.acme.autofinance.reporting.repository.LoanReportRepository;
import com.acme.autofinance.reporting.repository.ProcessedEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@Import({ReportService.class, ReportEventListener.class})
class ReportProjectionTest {

    @Autowired private ReportEventListener listener;
    @Autowired private ReportService reportService;
    @Autowired private LoanReportRepository loanRepository;
    @Autowired private AccountReportRepository accountRepository;
    @Autowired private ProcessedEventRepository processedEventRepository;

    @Test
    void authoritativeEventsBuildPortfolioDelinquencyAndDealerViews() {
        listener.on(new DealPackageSubmitted("DEAL-7", 7L, "VIN-7",
                money("12000.00"), money("1000.00"), money("500.00")));
        listener.on(new LoanApplicationSubmitted(101L, "APP-101", "Alex",
                money("10000.00"), "VIN-7", 7L));
        listener.on(new CreditDecisionMade(101L, "APPROVED", 720,
                "A", money("10000.00"), money("4.50")));
        listener.on(new LoanFunded(101L, "APP-101", LocalDate.of(2026, 7, 1),
                money("10000.00"), money("4.50"), 60));
        listener.on(new AccountCreated(501L, "ACC-501", 101L, "Alex", money("10000.00")));
        listener.on(new AccountBalanceUpdated(501L, money("10000.00"),
                money("9000.00"), money("-1000.00"), "PAYMENT"));
        listener.on(new AccountDelinquent(501L, 101L, 45, "31-60", money("9000.00")));
        listener.on(new PaymentReceived(701L, 101L, money("500.00"), "ACH", "CONF-701"));
        listener.on(new PaymentProcessed(701L, 101L, LocalDate.of(2026, 7, 10),
                "COMPLETED", money("450.00"), money("9000.00")));
        listener.on(new LateFeesAssessed(101L, money("35.00"), 45, LocalDate.of(2026, 7, 10)));
        listener.on(new DealerSettlementCalculated(7L, "DLR-7",
                money("300.00"), money("75.00"), money("225.00")));

        Map<String, Object> portfolio = reportService.getPortfolioSummary();
        assertEquals(1L, portfolio.get("totalLoansOriginated"));
        assertMoney("10000.00", portfolio.get("totalAmountOriginated"));
        assertMoney("9000.00", portfolio.get("currentPortfolioBalance"));
        assertEquals(0L, portfolio.get("activeAccounts"));
        assertEquals(1L, portfolio.get("delinquentAccounts"));
        assertMoney("500.00", portfolio.get("totalCollected"));

        Map<String, Object> delinquency = reportService.getDelinquencyReport();
        List<Map<String, Object>> buckets =
                (List<Map<String, Object>>) delinquency.get("delinquencyBuckets");
        assertEquals(4, buckets.size());
        assertEquals(1L, buckets.get(1).get("count"));
        assertMoney("9000.00", buckets.get(1).get("balance"));

        List<Map<String, Object>> dealers = reportService.getDealerPerformanceReport();
        assertEquals(1, dealers.size());
        assertEquals("DLR-7", dealers.get(0).get("dealer_code"));
        assertEquals(1L, dealers.get(0).get("total_deals"));
        assertEquals(1L, dealers.get(0).get("funded_deals"));
        assertMoney("12000.00", dealers.get(0).get("ytd_volume"));
        assertMoney("300.00", dealers.get(0).get("total_reserves"));
        assertMoney("35.00", loanRepository.findById(101L).get().getLateFeesAssessed());
        assertEquals(11L, processedEventRepository.count());
    }

    @Test
    void duplicateAndOutOfOrderEventsConvergeWithoutDoubleApplying() {
        LoanFunded funded = new LoanFunded(202L, "APP-202", LocalDate.of(2026, 7, 2),
                money("8000.00"), money("5.25"), 48);
        listener.on(funded);
        listener.on(funded);
        listener.on(new DealPackageSubmitted("DEAL-8", 8L, "VIN-8",
                money("9000.00"), money("500.00"), BigDecimal.ZERO));
        listener.on(new LoanApplicationSubmitted(202L, "APP-202", "Blair",
                money("8500.00"), "VIN-8", 8L));

        AccountBalanceUpdated balance = new AccountBalanceUpdated(
                502L, money("8000.00"), money("7500.00"), money("-500.00"), "PAYMENT");
        listener.on(balance);
        listener.on(balance);
        listener.on(new AccountCreated(502L, "ACC-502", 202L, "Blair", money("8000.00")));

        PaymentProcessed processed = new PaymentProcessed(
                702L, 202L, LocalDate.of(2026, 7, 11), "COMPLETED",
                money("225.00"), money("7500.00"));
        listener.on(processed);
        listener.on(processed);
        listener.on(new PaymentReceived(702L, 202L, money("250.00"), "ACH", "CONF-702"));

        AccountReportProjection account = accountRepository.findById(502L).get();
        assertMoney("7500.00", account.getCurrentBalance());

        Map<String, Object> portfolio = reportService.getPortfolioSummary();
        assertEquals(1L, portfolio.get("totalLoansOriginated"));
        assertMoney("8000.00", portfolio.get("totalAmountOriginated"));
        assertMoney("7500.00", portfolio.get("currentPortfolioBalance"));
        assertMoney("250.00", portfolio.get("totalCollected"));

        List<Map<String, Object>> dealers = reportService.getDealerPerformanceReport();
        assertEquals(1L, dealers.get(0).get("funded_deals"));
        assertMoney("9000.00", dealers.get(0).get("ytd_volume"));
        assertEquals(7L, processedEventRepository.count());
        assertTrue(loanRepository.findById(202L).get().isFunded());
    }

    private static BigDecimal money(String value) {
        return new BigDecimal(value);
    }

    private static void assertMoney(String expected, Object actual) {
        assertEquals(0, money(expected).compareTo((BigDecimal) actual));
    }
}
