package com.acme.payment.command;

import com.acme.payment.domain.Payment;
import com.acme.payment.domain.PaymentMethod;
import com.acme.payment.domain.PaymentStatus;
import com.acme.payment.event.LateFeesAssessed;
import com.acme.payment.event.PaymentAllocated;
import com.acme.payment.event.PaymentProcessed;
import com.acme.payment.event.PaymentReceived;
import com.acme.payment.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the payment write side. Verifies that each command persists the
 * aggregate AND publishes the right domain events with the right payloads — the contract
 * downstream services rely on.
 */
@SpringBootTest
@RecordApplicationEvents
class PaymentCommandServiceIntegrationTest {

    @Autowired
    private PaymentCommandService commandService;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private ApplicationEvents events;

    @Test
    void achPaymentEmitsReceivedAllocatedAndProcessedEvents() {
        SubmitPaymentCommand cmd = new SubmitPaymentCommand();
        cmd.setLoanId(100L);
        cmd.setPaymentAmount(new BigDecimal("500.00"));
        cmd.setPaymentMethod(PaymentMethod.ACH);
        cmd.setAchRoutingNumber("123456789");
        cmd.setOutstandingBalance(new BigDecimal("12000.00"));
        cmd.setAnnualInterestRate(new BigDecimal("6.00"));
        cmd.setOutstandingFees(new BigDecimal("25.00"));

        Payment payment = commandService.submitPayment(cmd);

        // Persisted and completed straight-through for ACH
        Payment stored = paymentRepository.findById(payment.getId()).get();
        assertThat(stored.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(stored.getConfirmationNumber()).startsWith("PMT-");

        // Allocation: $25 fees, then interest = 12000 * (6/1200) = $60, principal = $415
        assertThat(stored.getAllocation().getFeeAmount()).isEqualByComparingTo("25.00");
        assertThat(stored.getAllocation().getInterestAmount()).isEqualByComparingTo("60.00");
        assertThat(stored.getAllocation().getPrincipalAmount()).isEqualByComparingTo("415.00");

        // Events published in order
        assertThat(events.stream(PaymentReceived.class).count()).isEqualTo(1);
        PaymentAllocated allocated = events.stream(PaymentAllocated.class).findFirst().get();
        assertThat(allocated.getPrincipalAmount()).isEqualByComparingTo("415.00");
        assertThat(allocated.getInterestAmount()).isEqualByComparingTo("60.00");
        assertThat(allocated.getFeeAmount()).isEqualByComparingTo("25.00");

        PaymentProcessed processed = events.stream(PaymentProcessed.class).findFirst().get();
        assertThat(processed.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(processed.getPrincipalApplied()).isEqualByComparingTo("415.00");
        assertThat(processed.getLoanId()).isEqualTo(100L);
    }

    @Test
    void checkPaymentIsReceivedAndAllocatedButNotProcessedStraightThrough() {
        SubmitPaymentCommand cmd = new SubmitPaymentCommand();
        cmd.setLoanId(101L);
        cmd.setPaymentAmount(new BigDecimal("300.00"));
        cmd.setPaymentMethod(PaymentMethod.CHECK);
        cmd.setOutstandingBalance(new BigDecimal("6000.00"));
        cmd.setAnnualInterestRate(new BigDecimal("0.00"));

        Payment payment = commandService.submitPayment(cmd);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(events.stream(PaymentReceived.class).count()).isEqualTo(1);
        assertThat(events.stream(PaymentAllocated.class).count()).isEqualTo(1);
        assertThat(events.stream(PaymentProcessed.class).count()).isZero();
    }

    @Test
    void achPaymentWithInvalidRoutingFails() {
        SubmitPaymentCommand cmd = new SubmitPaymentCommand();
        cmd.setLoanId(102L);
        cmd.setPaymentAmount(new BigDecimal("200.00"));
        cmd.setPaymentMethod(PaymentMethod.ACH);
        cmd.setAchRoutingNumber("12345"); // too short
        cmd.setOutstandingBalance(new BigDecimal("4000.00"));
        cmd.setAnnualInterestRate(new BigDecimal("5.00"));

        Payment payment = commandService.submitPayment(cmd);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        PaymentProcessed processed = events.stream(PaymentProcessed.class).findFirst().get();
        assertThat(processed.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(processed.getPrincipalApplied()).isEqualByComparingTo("0");
    }

    @Test
    void lateFeeUnder30DaysIsFlatAndEmitsEvent() {
        AssessLateFeeCommand cmd = new AssessLateFeeCommand();
        cmd.setLoanId(200L);
        cmd.setDaysPastDue(15);
        cmd.setCurrentBalance(new BigDecimal("10000.00"));

        Payment fee = commandService.assessLateFee(cmd);

        assertThat(fee.getLateFee()).isEqualByComparingTo("25.00");
        assertThat(fee.getConfirmationNumber()).startsWith("FEE-");
        LateFeesAssessed event = events.stream(LateFeesAssessed.class).findFirst().get();
        assertThat(event.getFeeAmount()).isEqualByComparingTo("25.00");
        assertThat(event.getDaysPastDue()).isEqualTo(15);
    }

    @Test
    void lateFeeOver30DaysIsPercentageCappedAtMax() {
        AssessLateFeeCommand cmd = new AssessLateFeeCommand();
        cmd.setLoanId(201L);
        cmd.setDaysPastDue(45);
        cmd.setCurrentBalance(new BigDecimal("10000.00")); // 5% = 500, capped to 50

        Payment fee = commandService.assessLateFee(cmd);

        assertThat(fee.getLateFee()).isEqualByComparingTo("50.00");
    }

    @Test
    void paymentHistoryIsScopedToLoan() {
        SubmitPaymentCommand cmd = new SubmitPaymentCommand();
        cmd.setLoanId(300L);
        cmd.setPaymentAmount(new BigDecimal("100.00"));
        cmd.setPaymentMethod(PaymentMethod.WIRE);
        cmd.setOutstandingBalance(new BigDecimal("1000.00"));
        cmd.setAnnualInterestRate(new BigDecimal("0.00"));
        commandService.submitPayment(cmd);

        List<Payment> history = paymentRepository.findByLoanIdOrderByReceivedAtDesc(300L);
        assertThat(history).hasSize(1);
        assertThat(history.get(0).getLoanId()).isEqualTo(300L);
    }
}
