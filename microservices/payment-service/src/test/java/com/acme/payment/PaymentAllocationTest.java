package com.acme.payment;

import com.acme.payment.domain.Payment;
import com.acme.payment.domain.PaymentAllocation;
import com.acme.payment.domain.PaymentMethod;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit-level coverage of the in-domain allocation math (no Spring context).
 */
class PaymentAllocationTest {

    @Test
    void splitsAmountAcrossFeesInterestThenPrincipal() {
        Payment payment = Payment.submit(1L, new BigDecimal("500.00"),
                PaymentMethod.CHECK, null, null);

        // balance 10,000 @ 0.5% monthly => interest 50.00; outstanding fees 25.00
        PaymentAllocation allocation = payment.allocate(
                new BigDecimal("10000.00"), new BigDecimal("0.005"), new BigDecimal("25.00"));

        assertThat(allocation.getFeeAmount()).isEqualByComparingTo("25.00");
        assertThat(allocation.getInterestAmount()).isEqualByComparingTo("50.00");
        assertThat(allocation.getPrincipalAmount()).isEqualByComparingTo("425.00");
        assertThat(allocation.total()).isEqualByComparingTo("500.00");

        // Entity fields are updated to match the allocation.
        assertThat(payment.getFeeAmount()).isEqualByComparingTo("25.00");
        assertThat(payment.getInterestAmount()).isEqualByComparingTo("50.00");
        assertThat(payment.getPrincipalAmount()).isEqualByComparingTo("425.00");
    }

    @Test
    void lateFeeScheduleFlatUnder30Days() {
        assertThat(Payment.computeLateFee(new BigDecimal("9999.99"), 10))
                .isEqualByComparingTo("25.00");
    }

    @Test
    void lateFeeSchedulePercentCappedOver30Days() {
        // 5% of 600 = 30.00 (under the 50 cap)
        assertThat(Payment.computeLateFee(new BigDecimal("600.00"), 45))
                .isEqualByComparingTo("30.00");
        // 5% of 5000 = 250 -> capped at 50.00
        assertThat(Payment.computeLateFee(new BigDecimal("5000.00"), 60))
                .isEqualByComparingTo("50.00");
    }
}
