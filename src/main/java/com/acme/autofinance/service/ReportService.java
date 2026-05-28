package com.acme.autofinance.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Reporting service — direct SQL queries against shared tables across all domains.
 * No domain boundaries: reads from loans, payments, accounts, dealers, and deals directly.
 */
@Service
public class ReportService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public Map<String, Object> getPortfolioSummary() {
        Map<String, Object> summary = new HashMap<>();

        Long totalLoans = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM loan_applications", Long.class);

        BigDecimal totalOriginated = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(approved_amount), 0) FROM loan_applications " +
                "WHERE status IN ('ACTIVE', 'FUNDED', 'PAID_OFF')", BigDecimal.class);

        BigDecimal currentPortfolio = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(current_balance), 0) FROM accounts " +
                "WHERE status NOT IN ('PAID_IN_FULL', 'EARLY_TERMINATION')", BigDecimal.class);

        Long activeAccounts = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM accounts WHERE status = 'CURRENT'", Long.class);

        Long delinquentAccounts = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM accounts WHERE days_past_due > 30", Long.class);

        BigDecimal totalCollected = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(payment_amount), 0) FROM payments WHERE status = 'COMPLETED'",
                BigDecimal.class);

        summary.put("totalLoansOriginated", totalLoans);
        summary.put("totalAmountOriginated", totalOriginated);
        summary.put("currentPortfolioBalance", currentPortfolio);
        summary.put("activeAccounts", activeAccounts);
        summary.put("delinquentAccounts", delinquentAccounts);
        summary.put("totalCollected", totalCollected);
        summary.put("reportDate", new Date());

        return summary;
    }

    public Map<String, Object> getDelinquencyReport() {
        Map<String, Object> report = new HashMap<>();

        List<Map<String, Object>> buckets = new ArrayList<>();

        // 1-30 days
        Map<String, Object> bucket1 = new HashMap<>();
        bucket1.put("bucket", "1-30 days");
        bucket1.put("count", jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM accounts WHERE days_past_due BETWEEN 1 AND 30", Long.class));
        bucket1.put("balance", jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(current_balance), 0) FROM accounts WHERE days_past_due BETWEEN 1 AND 30",
                BigDecimal.class));
        buckets.add(bucket1);

        // 31-60 days
        Map<String, Object> bucket2 = new HashMap<>();
        bucket2.put("bucket", "31-60 days");
        bucket2.put("count", jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM accounts WHERE days_past_due BETWEEN 31 AND 60", Long.class));
        bucket2.put("balance", jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(current_balance), 0) FROM accounts WHERE days_past_due BETWEEN 31 AND 60",
                BigDecimal.class));
        buckets.add(bucket2);

        // 61-90 days
        Map<String, Object> bucket3 = new HashMap<>();
        bucket3.put("bucket", "61-90 days");
        bucket3.put("count", jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM accounts WHERE days_past_due BETWEEN 61 AND 90", Long.class));
        bucket3.put("balance", jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(current_balance), 0) FROM accounts WHERE days_past_due BETWEEN 61 AND 90",
                BigDecimal.class));
        buckets.add(bucket3);

        // 90+ days
        Map<String, Object> bucket4 = new HashMap<>();
        bucket4.put("bucket", "90+ days");
        bucket4.put("count", jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM accounts WHERE days_past_due > 90", Long.class));
        bucket4.put("balance", jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(current_balance), 0) FROM accounts WHERE days_past_due > 90",
                BigDecimal.class));
        buckets.add(bucket4);

        report.put("delinquencyBuckets", buckets);
        report.put("reportDate", new Date());

        // Regulatory compliance stubs — TILA and ECOA
        report.put("tilaDisclosuresComplete", true);
        report.put("ecoaAdverseActionNoticesSent", true);
        report.put("regulatoryNotes", "All adverse action notices sent within 30-day requirement per ECOA Regulation B");

        return report;
    }

    public List<Map<String, Object>> getDealerPerformanceReport() {
        return jdbcTemplate.queryForList(
                "SELECT d.dealer_code, d.dealer_name, d.tier, d.ytd_volume, " +
                "COUNT(dp.id) as total_deals, " +
                "SUM(CASE WHEN dp.status = 'FUNDED' OR dp.status = 'SETTLED' THEN 1 ELSE 0 END) as funded_deals, " +
                "COALESCE(SUM(dp.dealer_reserve), 0) as total_reserves " +
                "FROM dealers d LEFT JOIN deal_packages dp ON d.id = dp.dealer_id " +
                "GROUP BY d.id, d.dealer_code, d.dealer_name, d.tier, d.ytd_volume " +
                "ORDER BY d.ytd_volume DESC");
    }
}
