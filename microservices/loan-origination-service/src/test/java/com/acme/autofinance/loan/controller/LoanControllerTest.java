package com.acme.autofinance.loan.controller;

import com.acme.autofinance.loan.domain.LoanApplication;
import com.acme.autofinance.loan.domain.LoanStatus;
import com.acme.autofinance.loan.service.LoanNotFoundException;
import com.acme.autofinance.loan.service.LoanService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Arrays;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contract tests for the six retained loan-origination endpoints, plus a guard
 * proving the cross-domain {@code /api/loans/end-of-day} endpoint is not exposed.
 */
@WebMvcTest(LoanController.class)
@Import(LoanExceptionHandler.class)
class LoanControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LoanService loanService;

    private LoanApplication sample(Long id, String number, LoanStatus status) {
        LoanApplication a = new LoanApplication();
        a.setId(id);
        a.setApplicationNumber(number);
        a.setApplicantName("Jordan Rivera");
        a.setRequestedAmount(new BigDecimal("30000.00"));
        a.setStatus(status);
        return a;
    }

    @Test
    void createApplicationReturns201() throws Exception {
        when(loanService.createApplication(any(LoanApplication.class)))
                .thenReturn(sample(1L, "LN-0001", LoanStatus.APPROVED));

        mockMvc.perform(post("/api/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"applicantName\":\"Jordan Rivera\",\"requestedAmount\":30000.00}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.applicationNumber").value("LN-0001"))
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    void createApplicationRejectsMissingApplicantName() throws Exception {
        mockMvc.perform(post("/api/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"requestedAmount\":30000.00}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Applicant name is required"));
    }

    @Test
    void createApplicationRejectsNonPositiveAmount() throws Exception {
        mockMvc.perform(post("/api/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"applicantName\":\"Jordan Rivera\",\"requestedAmount\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Requested amount must be positive"));
    }

    @Test
    void getApplicationReturnsLoan() throws Exception {
        when(loanService.getApplication(eq(1L))).thenReturn(sample(1L, "LN-0001", LoanStatus.APPROVED));

        mockMvc.perform(get("/api/loans/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.applicationNumber").value("LN-0001"));
    }

    @Test
    void getApplicationReturns404WhenMissing() throws Exception {
        when(loanService.getApplication(eq(99L)))
                .thenThrow(new LoanNotFoundException("Loan application not found: 99"));

        mockMvc.perform(get("/api/loans/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Loan application not found: 99"));
    }

    @Test
    void getByNumberReturnsLoan() throws Exception {
        when(loanService.getByApplicationNumber(eq("LN-0001")))
                .thenReturn(sample(1L, "LN-0001", LoanStatus.APPROVED));

        mockMvc.perform(get("/api/loans/number/LN-0001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applicationNumber").value("LN-0001"));
    }

    @Test
    void listAllApplications() throws Exception {
        when(loanService.getAllApplications())
                .thenReturn(Arrays.asList(sample(1L, "LN-0001", LoanStatus.APPROVED),
                        sample(2L, "LN-0002", LoanStatus.DECLINED)));

        mockMvc.perform(get("/api/loans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].applicationNumber").value("LN-0001"));
    }

    @Test
    void listApplicationsByStatus() throws Exception {
        when(loanService.getByStatus(eq(LoanStatus.APPROVED)))
                .thenReturn(Arrays.asList(sample(1L, "LN-0001", LoanStatus.APPROVED)));

        mockMvc.perform(get("/api/loans").param("status", "APPROVED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("APPROVED"));
    }

    @Test
    void updateTermsReturnsUpdatedLoan() throws Exception {
        LoanApplication updated = sample(1L, "LN-0001", LoanStatus.APPROVED);
        updated.setTermMonths(72);
        when(loanService.updateTerms(eq(1L), any(), any(), eq(72))).thenReturn(updated);

        mockMvc.perform(put("/api/loans/1/terms").param("termMonths", "72"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.termMonths").value(72));
    }

    @Test
    void fundLoanReturnsFundedLoan() throws Exception {
        when(loanService.fundLoan(eq(1L))).thenReturn(sample(1L, "LN-0001", LoanStatus.ACTIVE));

        mockMvc.perform(post("/api/loans/1/fund"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void endOfDayEndpointIsNotExposed() throws Exception {
        // The cross-domain POST /api/loans/end-of-day batch endpoint was removed.
        // No POST handler exists for this path, so the request is rejected.
        mockMvc.perform(post("/api/loans/end-of-day"))
                .andExpect(status().isMethodNotAllowed());
    }
}
