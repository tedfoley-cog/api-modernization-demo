package com.acme.payment.web;

import com.acme.payment.event.PaymentProcessed;
import com.acme.payment.event.PaymentReceived;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end HTTP tests for the payment context, including verification that the REST
 * flow publishes the expected domain events.
 */
@SpringBootTest
@AutoConfigureMockMvc
@RecordApplicationEvents
class PaymentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ApplicationEvents events;

    @Test
    void submitAchPaymentReturnsCreatedAndPublishesEvents() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("loanId", 900);
        body.put("paymentAmount", 750.00);
        body.put("paymentMethod", "ACH");
        body.put("achRoutingNumber", "123456789");
        body.put("outstandingBalance", 12000.00);
        body.put("annualInterestRate", 6.00);
        body.put("outstandingFees", 0.00);

        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.confirmationNumber").exists())
                .andExpect(jsonPath("$.interestAmount").value(60.00));

        assertThat(events.stream(PaymentReceived.class).count()).isEqualTo(1);
        assertThat(events.stream(PaymentProcessed.class).count()).isEqualTo(1);
    }

    @Test
    void invalidPaymentIsRejectedWithBadRequest() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("loanId", 901);
        body.put("paymentAmount", -5.00); // invalid

        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());

        assertThat(events.stream(PaymentReceived.class).count()).isZero();
    }

    @Test
    void paymentHistoryEndpointReturnsSubmittedPayments() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("loanId", 902);
        body.put("paymentAmount", 250.00);
        body.put("paymentMethod", "CHECK");
        body.put("outstandingBalance", 5000.00);
        body.put("annualInterestRate", 0.00);

        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/payments/loan/902"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].loanId").value(902))
                .andExpect(jsonPath("$[0].status").value("PENDING"));
    }
}
