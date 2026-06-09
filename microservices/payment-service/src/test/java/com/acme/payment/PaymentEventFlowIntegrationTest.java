package com.acme.payment;

import com.acme.payment.command.AssessLateFeeCommand;
import com.acme.payment.command.PaymentCommandService;
import com.acme.payment.command.SubmitPaymentCommand;
import com.acme.payment.domain.Payment;
import com.acme.payment.domain.PaymentMethod;
import com.acme.payment.domain.PaymentStatus;
import com.acme.payment.event.LateFeesAssessed;
import com.acme.payment.event.PaymentAllocated;
import com.acme.payment.event.PaymentProcessed;
import com.acme.payment.event.PaymentReceived;
import com.acme.payment.support.RecordingEventListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests exercising the payment service through the command side and asserting
 * that the expected domain events are published (captured via a recording listener).
 */
@SpringBootTest
class PaymentEventFlowIntegrationTest {

    @Autowired
    private PaymentCommandService commandService;

    @Autowired
    private RecordingEventListener recordingListener;

    @BeforeEach
    void resetListener() {
        recordingListener.clear();
    }

    @Test
    void achHappyPathPublishesReceivedAllocatedProcessed() {
        SubmitPaymentCommand command = new SubmitPaymentCommand(
                1001L,
                new BigDecimal("500.00"),
                PaymentMethod.ACH,
                "123456789",            // valid 9-digit routing number
                "000111222",
                new BigDecimal("10000.00"),
                new BigDecimal("12.00"));

        Payment saved = commandService.submitPayment(command);

        assertThat(saved.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(recordingListener.typesInOrder())
                .containsExactly("payment.received", "payment.allocated", "payment.processed");

        PaymentReceived received = recordingListener.ofType(PaymentReceived.class).get(0);
        assertThat(received.getLoanId()).isEqualTo(1001L);
        assertThat(received.getPaymentMethod()).isEqualTo(PaymentMethod.ACH);

        PaymentAllocated allocated = recordingListener.ofType(PaymentAllocated.class).get(0);
        assertThat(allocated.getInterestAmount()).isEqualByComparingTo("100.00");
        assertThat(allocated.getPrincipalAmount()).isEqualByComparingTo("400.00");

        PaymentProcessed processed = recordingListener.ofType(PaymentProcessed.class).get(0);
        assertThat(processed.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(processed.getPrincipalApplied()).isEqualByComparingTo("400.00");
    }

    @Test
    void achWithInvalidRoutingNumberFailsAndPublishesProcessedFailed() {
        SubmitPaymentCommand command = new SubmitPaymentCommand(
                1002L,
                new BigDecimal("250.00"),
                PaymentMethod.ACH,
                "12345",                // invalid (not 9 digits)
                "000111222",
                new BigDecimal("5000.00"),
                new BigDecimal("6.00"));

        Payment saved = commandService.submitPayment(command);

        assertThat(saved.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(recordingListener.typesInOrder())
                .containsExactly("payment.received", "payment.allocated", "payment.processed");
        assertThat(recordingListener.ofType(PaymentProcessed.class).get(0).getStatus())
                .isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    void lateFeePathPublishesLateFeesAssessed() {
        // <= 30 days past due => flat $25
        AssessLateFeeCommand flat = new AssessLateFeeCommand(2001L, 15, new BigDecimal("8000.00"));
        commandService.assessLateFee(flat);

        assertThat(recordingListener.typesInOrder()).containsExactly("payment.late_fees_assessed");
        LateFeesAssessed event = recordingListener.ofType(LateFeesAssessed.class).get(0);
        assertThat(event.getLoanId()).isEqualTo(2001L);
        assertThat(event.getLateFee()).isEqualByComparingTo("25.00");
    }

    @Test
    void lateFeeOverThirtyDaysUsesPercentageCappedAtFifty() {
        // > 30 days: 5% of 8000 = 400, capped at 50
        AssessLateFeeCommand pct = new AssessLateFeeCommand(2002L, 45, new BigDecimal("8000.00"));
        Payment fee = commandService.assessLateFee(pct);

        assertThat(fee.getLateFee()).isEqualByComparingTo("50.00");
        assertThat(recordingListener.ofType(LateFeesAssessed.class).get(0).getLateFee())
                .isEqualByComparingTo("50.00");
    }
}
