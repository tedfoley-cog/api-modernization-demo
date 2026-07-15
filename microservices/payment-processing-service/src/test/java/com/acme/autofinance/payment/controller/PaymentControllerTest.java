package com.acme.autofinance.payment.controller;

import com.acme.autofinance.payment.domain.Payment;
import com.acme.autofinance.payment.domain.PaymentMethod;
import com.acme.autofinance.payment.domain.PaymentStatus;
import com.acme.autofinance.payment.service.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentController.class)
@Import(PaymentExceptionHandler.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PaymentService paymentService;

    private Payment sample(Long id, Long loanId) {
        Payment p = new Payment();
        p.setId(id);
        p.setLoanId(loanId);
        p.setPaymentAmount(new BigDecimal("450.00"));
        p.setPaymentMethod(PaymentMethod.ACH);
        p.setStatus(PaymentStatus.PENDING);
        p.setConfirmationNumber("PMT-TEST0001");
        return p;
    }

    @Test
    void submitPaymentReturns201WithBody() throws Exception {
        Payment request = new Payment();
        request.setLoanId(10L);
        request.setPaymentAmount(new BigDecimal("450.00"));
        request.setPaymentMethod(PaymentMethod.ACH);

        when(paymentService.submitPayment(any(Payment.class))).thenReturn(sample(1L, 10L));

        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.loanId").value(10))
                .andExpect(jsonPath("$.confirmationNumber").value("PMT-TEST0001"));
    }

    @Test
    void submitPaymentRejectsMissingLoanId() throws Exception {
        Payment request = new Payment();
        request.setPaymentAmount(new BigDecimal("450.00"));

        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Loan ID is required"));
    }

    @Test
    void submitPaymentRejectsNonPositiveAmount() throws Exception {
        Payment request = new Payment();
        request.setLoanId(10L);
        request.setPaymentAmount(BigDecimal.ZERO);

        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Payment amount must be positive"));
    }

    @Test
    void getPaymentHistoryReturnsList() throws Exception {
        when(paymentService.getPaymentHistory(eq(10L)))
                .thenReturn(Arrays.asList(sample(1L, 10L), sample(2L, 10L)));

        mockMvc.perform(get("/api/payments/loan/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].loanId").value(10));
    }

    @Test
    void processBatchReturnsSummary() throws Exception {
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalSubmitted", 2);
        summary.put("processed", 1);
        summary.put("failed", 1);
        when(paymentService.processBatchPayments(any())).thenReturn(summary);

        List<Payment> batch = Arrays.asList(sample(null, 10L), sample(null, 11L));

        mockMvc.perform(post("/api/payments/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(batch)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSubmitted").value(2))
                .andExpect(jsonPath("$.processed").value(1))
                .andExpect(jsonPath("$.failed").value(1));
    }

    @Test
    void getPendingPaymentsReturnsList() throws Exception {
        when(paymentService.getPendingPayments()).thenReturn(Collections.singletonList(sample(5L, 12L)));

        mockMvc.perform(get("/api/payments/pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(5));
    }
}
