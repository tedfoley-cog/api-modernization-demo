package com.acme.payment.service;

import com.acme.payment.domain.model.PaymentAllocation;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Payment math extracted from the monolith's LoanService.
 * Owns monthly payment and payoff calculations.
 */
@Service
public class PaymentCalculationService {

    /**
     * Standard amortization formula: M = P * [r(1+r)^n] / [(1+r)^n - 1]
     */
    public BigDecimal calculateMonthlyPayment(BigDecimal principal, BigDecimal annualRate, int termMonths) {
        if (principal == null || annualRate == null || termMonths <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal monthlyRate = annualRate.divide(new BigDecimal("1200"), 10, RoundingMode.HALF_UP);

        if (monthlyRate.compareTo(BigDecimal.ZERO) == 0) {
            return principal.divide(new BigDecimal(termMonths), 2, RoundingMode.HALF_UP);
        }

        BigDecimal onePlusR = BigDecimal.ONE.add(monthlyRate);
        BigDecimal power = onePlusR.pow(termMonths, new MathContext(15));
        BigDecimal numerator = principal.multiply(monthlyRate).multiply(power);
        BigDecimal denominator = power.subtract(BigDecimal.ONE);

        return numerator.divide(denominator, 2, RoundingMode.HALF_UP);
    }

    /**
     * Total payoff = monthly payment * term months.
     */
    public BigDecimal calculatePayoffAmount(BigDecimal approvedAmount, BigDecimal interestRate, Integer termMonths) {
        if (approvedAmount == null || interestRate == null) {
            return approvedAmount != null ? approvedAmount : BigDecimal.ZERO;
        }
        int term = termMonths != null ? termMonths : 60;
        BigDecimal monthly = calculateMonthlyPayment(approvedAmount, interestRate, term);
        return monthly.multiply(new BigDecimal(term)).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Allocate a payment amount across fees, interest, and principal.
     * Order: outstanding late fees first, then interest, then principal.
     */
    public PaymentAllocation allocatePayment(BigDecimal totalAmount, BigDecimal outstandingFees,
                                             BigDecimal interestPortion) {
        BigDecimal remaining = totalAmount;

        BigDecimal feeAllocation = outstandingFees.min(remaining);
        remaining = remaining.subtract(feeAllocation);

        BigDecimal interestAllocation = interestPortion.min(remaining);
        remaining = remaining.subtract(interestAllocation);

        BigDecimal principalAllocation = remaining;

        return new PaymentAllocation(principalAllocation, interestAllocation, feeAllocation);
    }
}
