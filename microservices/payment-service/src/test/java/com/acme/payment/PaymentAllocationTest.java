package com.acme.payment;

import com.acme.payment.domain.PaymentAllocation;
import com.acme.payment.domain.PaymentAllocator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the allocation logic (late fees -> interest -> principal), preserving
 * the monolith's behavior without any Spring context or database.
 */
class PaymentAllocationTest {

    @Test
    void splitsFeesThenInterestThenPrincipal() {
        // balance 10,000 @ 12% annual => monthly rate 1% => interest 100.00
        // outstanding late fees 25.00
        PaymentAllocation result = PaymentAllocator.allocate(
                new BigDecimal("500.00"),
                new BigDecimal("10000.00"),
                new BigDecimal("12.00"),
                new BigDecimal("25.00"));

        assertThat(result.getFeeAmount()).isEqualByComparingTo("25.00");
        assertThat(result.getInterestAmount()).isEqualByComparingTo("100.00");
        // 500 - 25 - 100 = 375 to principal
        assertThat(result.getPrincipalAmount()).isEqualByComparingTo("375.00");
    }

    @Test
    void feesAreCappedAtTheTotalAmount() {
        // payment smaller than outstanding fees: all goes to fees, nothing to interest/principal
        PaymentAllocation result = PaymentAllocator.allocate(
                new BigDecimal("20.00"),
                new BigDecimal("10000.00"),
                new BigDecimal("12.00"),
                new BigDecimal("25.00"));

        assertThat(result.getFeeAmount()).isEqualByComparingTo("20.00");
        assertThat(result.getInterestAmount()).isEqualByComparingTo("0.00");
        assertThat(result.getPrincipalAmount()).isEqualByComparingTo("0.00");
    }

    @Test
    void interestIsCappedAtRemainingAfterFees() {
        // payment 80, fees 25 -> remaining 55; interest would be 100 but capped to 55; principal 0
        PaymentAllocation result = PaymentAllocator.allocate(
                new BigDecimal("80.00"),
                new BigDecimal("10000.00"),
                new BigDecimal("12.00"),
                new BigDecimal("25.00"));

        assertThat(result.getFeeAmount()).isEqualByComparingTo("25.00");
        assertThat(result.getInterestAmount()).isEqualByComparingTo("55.00");
        assertThat(result.getPrincipalAmount()).isEqualByComparingTo("0.00");
    }

    @Test
    void noFeesOrInterestSendsEverythingToPrincipal() {
        PaymentAllocation result = PaymentAllocator.allocate(
                new BigDecimal("300.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO);

        assertThat(result.getFeeAmount()).isEqualByComparingTo("0.00");
        assertThat(result.getInterestAmount()).isEqualByComparingTo("0.00");
        assertThat(result.getPrincipalAmount()).isEqualByComparingTo("300.00");
    }
}
