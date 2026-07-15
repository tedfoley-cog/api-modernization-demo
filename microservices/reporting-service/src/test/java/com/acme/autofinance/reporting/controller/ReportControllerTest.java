package com.acme.autofinance.reporting.controller;

import com.acme.autofinance.reporting.service.ReportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReportController.class)
class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReportService reportService;

    @Test
    void portfolioEndpointPreservesSummaryContract() throws Exception {
        Map<String, Object> portfolio = new LinkedHashMap<>();
        portfolio.put("totalLoansOriginated", 12L);
        portfolio.put("totalAmountOriginated", new BigDecimal("250000.00"));
        portfolio.put("currentPortfolioBalance", new BigDecimal("180000.00"));
        portfolio.put("activeAccounts", 9L);
        portfolio.put("delinquentAccounts", 2L);
        portfolio.put("totalCollected", new BigDecimal("70000.00"));
        when(reportService.getPortfolioSummary()).thenReturn(portfolio);

        mockMvc.perform(get("/api/reports/portfolio"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalLoansOriginated").value(12))
                .andExpect(jsonPath("$.totalAmountOriginated").value(250000.00))
                .andExpect(jsonPath("$.currentPortfolioBalance").value(180000.00))
                .andExpect(jsonPath("$.activeAccounts").value(9))
                .andExpect(jsonPath("$.delinquentAccounts").value(2))
                .andExpect(jsonPath("$.totalCollected").value(70000.00));
    }

    @Test
    void delinquencyEndpointPreservesBucketAndComplianceContract() throws Exception {
        Map<String, Object> bucket = new LinkedHashMap<>();
        bucket.put("bucket", "31-60 days");
        bucket.put("count", 2L);
        bucket.put("balance", new BigDecimal("19000.00"));
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("delinquencyBuckets", Collections.singletonList(bucket));
        report.put("tilaDisclosuresComplete", true);
        report.put("ecoaAdverseActionNoticesSent", true);
        report.put("regulatoryNotes", "notices sent");
        when(reportService.getDelinquencyReport()).thenReturn(report);

        mockMvc.perform(get("/api/reports/delinquency"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.delinquencyBuckets[0].bucket").value("31-60 days"))
                .andExpect(jsonPath("$.delinquencyBuckets[0].count").value(2))
                .andExpect(jsonPath("$.delinquencyBuckets[0].balance").value(19000.00))
                .andExpect(jsonPath("$.tilaDisclosuresComplete").value(true))
                .andExpect(jsonPath("$.ecoaAdverseActionNoticesSent").value(true));
    }

    @Test
    void dealersEndpointPreservesDealerPerformanceContract() throws Exception {
        Map<String, Object> dealer = new LinkedHashMap<>();
        dealer.put("dealer_code", "DLR-7");
        dealer.put("dealer_name", "DLR-7");
        dealer.put("tier", "UNCLASSIFIED");
        dealer.put("ytd_volume", new BigDecimal("42000.00"));
        dealer.put("total_deals", 3L);
        dealer.put("funded_deals", 2L);
        dealer.put("total_reserves", new BigDecimal("900.00"));
        List<Map<String, Object>> dealers = new ArrayList<>();
        dealers.add(dealer);
        when(reportService.getDealerPerformanceReport()).thenReturn(dealers);

        mockMvc.perform(get("/api/reports/dealers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].dealer_code").value("DLR-7"))
                .andExpect(jsonPath("$[0].dealer_name").value("DLR-7"))
                .andExpect(jsonPath("$[0].tier").value("UNCLASSIFIED"))
                .andExpect(jsonPath("$[0].ytd_volume").value(42000.00))
                .andExpect(jsonPath("$[0].total_deals").value(3))
                .andExpect(jsonPath("$[0].funded_deals").value(2))
                .andExpect(jsonPath("$[0].total_reserves").value(900.00));
    }
}
