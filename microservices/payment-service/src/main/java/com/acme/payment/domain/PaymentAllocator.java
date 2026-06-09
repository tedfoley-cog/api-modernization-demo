package com.acme.payment.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Pure allocation logic extracted from the monolith's {@code PaymentService.allocatePayment}.
 *
 * <p>Allocation order is preserved exactly: <b>late fees &rarr; interest &rarr; principal</b>.
 * Unlike the monolith, this class performs no repository or cross-domain lookups — the
 * caller supplies the outstanding-balance and interest-rate context, which keeps the
 * payment domain decoupled from the loan and account domains.
 */
public final class PaymentAllocator {

    private static final BigDecimal MONTHS_PER_YEAR_TIMES_HUNDRED = new BigDecimal("1200");

    private PaymentAllocator() {}

    /**
     * Split {@code totalAmount} into fees, interest and principal.
     *
     * @param totalAmount         the payment amount being allocated
     * @param outstandingBalance  current outstanding balance (supplied by the account
     *                            domain via command input, not looked up here)
     * @param annualInterestRate  annual interest rate as a percentage (e.g. 6.00 for 6%)
     * @param outstandingLateFees sum of unpaid late fees already assessed on the loan
     *                            (sourced from the payment domain's own store)
     */
    public static PaymentAllocation allocate(BigDecimal totalAmount,
                                             BigDecimal outstandingBalance,
                                             BigDecimal annualInterestRate,
                                             BigDecimal outstandingLateFees) {
        BigDecimal total = totalAmount == null ? BigDecimal.ZERO : totalAmount;

        // Interest portion: outstanding balance * monthly rate.
        BigDecimal monthlyRate = BigDecimal.ZERO;
        if (annualInterestRate != null) {
            monthlyRate = annualInterestRate.divide(MONTHS_PER_YEAR_TIMES_HUNDRED, 10, RoundingMode.HALF_UP);
        }
        BigDecimal interestPortion = BigDecimal.ZERO;
        if (outstandingBalance != null) {
            interestPortion = outstandingBalance.multiply(monthlyRate).setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal remaining = total;

        // 1. Late fees first.
        BigDecimal feesPortion = outstandingLateFees == null ? BigDecimal.ZERO : outstandingLateFees;
        if (feesPortion.compareTo(remaining) > 0) {
            feesPortion = remaining;
        }
        remaining = remaining.subtract(feesPortion);

        // 2. Then interest.
        if (interestPortion.compareTo(remaining) > 0) {
            interestPortion = remaining;
        }
        remaining = remaining.subtract(interestPortion);

        // 3. Remainder to principal.
        BigDecimal principalPortion = remaining;

        return new PaymentAllocation(feesPortion, interestPortion, principalPortion);
    }
}
