package com.acme.payment;

import com.acme.payment.command.AssessLateFeeCommand;
import com.acme.payment.command.PaymentCommandService;
import com.acme.payment.command.SubmitPaymentCommand;
import com.acme.payment.domain.PaymentMethod;
import com.acme.payment.domain.PaymentStatus;
import com.acme.payment.event.LateFeesAssessed;
import com.acme.payment.event.PaymentAllocated;
import com.acme.payment.event.PaymentProcessed;
import com.acme.payment.event.PaymentReceived;
import com.acme.payment.repository.PaymentRepository;
import com.acme.payment.support.RecordingEventListener;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(RecordingEventListener.Config.class)
class PaymentServiceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RecordingEventListener events;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PaymentCommandService commandService;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
        events.clear();
    }

    private SubmitPaymentCommand sampleCommand(PaymentMethod method) {
        SubmitPaymentCommand cmd = new SubmitPaymentCommand();
        cmd.setLoanId(1001L);
        cmd.setPaymentAmount(new BigDecimal("500.00"));
        cmd.setPaymentMethod(method);
        cmd.setOutstandingBalance(new BigDecimal("10000.00"));
        cmd.setMonthlyInterestRate(new BigDecimal("0.005"));
        cmd.setOutstandingFees(new BigDecimal("25.00"));
        if (method == PaymentMethod.ACH) {
            cmd.setAchRoutingNumber("021000021");
            cmd.setAchAccountNumber("123456789");
        }
        return cmd;
    }

    @Test
    void submitPaymentReturnsCreatedAndPublishesReceivedAndAllocated() throws Exception {
        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleCommand(PaymentMethod.CHECK))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.confirmationNumber").exists())
                .andExpect(jsonPath("$.confirmationNumber").value(org.hamcrest.Matchers.startsWith("PMT-")))
                .andExpect(jsonPath("$.status").value("PENDING"));

        assertThat(events.ofType(PaymentReceived.class)).hasSize(1);
        assertThat(events.ofType(PaymentAllocated.class)).hasSize(1);
        // A non-ACH payment is not processed synchronously.
        assertThat(events.ofType(PaymentProcessed.class)).isEmpty();

        PaymentReceived received = events.ofType(PaymentReceived.class).get(0);
        assertThat(received.getLoanId()).isEqualTo(1001L);
        assertThat(received.getPaymentAmount()).isEqualByComparingTo("500.00");
        assertThat(received.getConfirmationNumber()).startsWith("PMT-");

        PaymentAllocated allocated = events.ofType(PaymentAllocated.class).get(0);
        assertThat(allocated.getFeeAmount()).isEqualByComparingTo("25.00");
        assertThat(allocated.getInterestAmount()).isEqualByComparingTo("50.00");
        assertThat(allocated.getPrincipalAmount()).isEqualByComparingTo("425.00");
    }

    @Test
    void achPaymentAdditionallyPublishesProcessed() throws Exception {
        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleCommand(PaymentMethod.ACH))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        assertThat(events.ofType(PaymentReceived.class)).hasSize(1);
        assertThat(events.ofType(PaymentAllocated.class)).hasSize(1);
        assertThat(events.ofType(PaymentProcessed.class)).hasSize(1);

        PaymentProcessed processed = events.ofType(PaymentProcessed.class).get(0);
        assertThat(processed.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(processed.getPrincipalApplied()).isEqualByComparingTo("425.00");
        // new balance = 10,000 - 425 principal applied
        assertThat(processed.getNewBalance()).isEqualByComparingTo("9575.00");
    }

    @Test
    void achWithInvalidRoutingNumberFailsAndPublishesNoProcessedEvent() throws Exception {
        SubmitPaymentCommand bad = sampleCommand(PaymentMethod.ACH);
        bad.setAchRoutingNumber("123"); // invalid: not 9 digits

        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bad)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("FAILED"));

        // Receipt + allocation still happen, but a failed settlement must NOT
        // emit PaymentProcessed (it would carry a null processedDate).
        assertThat(events.ofType(PaymentReceived.class)).hasSize(1);
        assertThat(events.ofType(PaymentAllocated.class)).hasSize(1);
        assertThat(events.ofType(PaymentProcessed.class)).isEmpty();
    }

    @Test
    void assessingLateFeePublishesLateFeesAssessedWithScheduleAmount() {
        AssessLateFeeCommand flat = new AssessLateFeeCommand();
        flat.setLoanId(2002L);
        flat.setDaysPastDue(10);
        flat.setOutstandingBalance(new BigDecimal("8000.00"));
        commandService.assessLateFee(flat);

        AssessLateFeeCommand pct = new AssessLateFeeCommand();
        pct.setLoanId(2002L);
        pct.setDaysPastDue(45);
        pct.setOutstandingBalance(new BigDecimal("600.00"));
        commandService.assessLateFee(pct);

        List<LateFeesAssessed> assessed = events.ofType(LateFeesAssessed.class);
        assertThat(assessed).hasSize(2);
        assertThat(assessed.get(0).getFeeAmount()).isEqualByComparingTo("25.00");
        assertThat(assessed.get(0).getDaysPastDue()).isEqualTo(10);
        assertThat(assessed.get(1).getFeeAmount()).isEqualByComparingTo("30.00");
        assertThat(assessed.get(1).getDaysPastDue()).isEqualTo(45);
    }

    @Test
    void queryHistoryAndPendingReturnExpectedRows() throws Exception {
        // Two CHECK payments (stay PENDING) for loan 3003 + one ACH (COMPLETED).
        commandService.submit(buildFor(3003L, PaymentMethod.CHECK));
        commandService.submit(buildFor(3003L, PaymentMethod.CHECK));
        commandService.submit(buildFor(3003L, PaymentMethod.ACH));
        // A payment for an unrelated loan.
        commandService.submit(buildFor(4004L, PaymentMethod.CHECK));

        mockMvc.perform(get("/api/payments/loan/{loanId}", 3003L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].loanId").value(3003));

        // Pending = the 3 CHECK payments (2 for loan 3003 + 1 for 4004); ACH completed.
        mockMvc.perform(get("/api/payments/pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
    }

    private SubmitPaymentCommand buildFor(Long loanId, PaymentMethod method) {
        SubmitPaymentCommand cmd = sampleCommand(method);
        cmd.setLoanId(loanId);
        return cmd;
    }
}
