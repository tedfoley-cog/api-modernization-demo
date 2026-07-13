package com.acme.payment.service;

import com.acme.payment.domain.model.Payment;
import com.acme.payment.domain.model.PaymentStatus;
import com.acme.payment.domain.model.PaymentSummary;
import com.acme.payment.domain.repository.PaymentRepository;
import com.acme.payment.domain.repository.PaymentSummaryRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * CQRS read side: queries the payment read model and payment history.
 * Replaces the monolith's direct SQL joins in ReportService/LoanService.
 */
@Service
public class PaymentQueryService {

    private final PaymentRepository paymentRepository;
    private final PaymentSummaryRepository summaryRepository;

    public PaymentQueryService(PaymentRepository paymentRepository,
                               PaymentSummaryRepository summaryRepository) {
        this.paymentRepository = paymentRepository;
        this.summaryRepository = summaryRepository;
    }

    public List<Payment> getPaymentHistory(Long loanId) {
        return paymentRepository.findByLoanId(loanId);
    }

    public List<Payment> getPendingPayments() {
        return paymentRepository.findByStatus(PaymentStatus.PENDING);
    }

    public Optional<PaymentSummary> getPaymentSummary(Long loanId) {
        return summaryRepository.findById(loanId);
    }

    public List<PaymentSummary> getAllSummaries() {
        return summaryRepository.findAll();
    }

    /**
     * Aggregate payment metrics for portfolio reporting.
     * Replaces the monolith's direct SQL: SELECT SUM(payment_amount) FROM payments WHERE status = 'COMPLETED'
     */
    public Map<String, Object> getPaymentMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        List<PaymentSummary> summaries = summaryRepository.findAll();
        BigDecimal totalCollected = BigDecimal.ZERO;
        BigDecimal totalPrincipal = BigDecimal.ZERO;
        BigDecimal totalInterest = BigDecimal.ZERO;
        BigDecimal totalFees = BigDecimal.ZERO;
        BigDecimal totalOutstandingLateFees = BigDecimal.ZERO;
        int totalPaymentCount = 0;

        for (PaymentSummary summary : summaries) {
            if (summary.getTotalPaid() != null) totalCollected = totalCollected.add(summary.getTotalPaid());
            if (summary.getTotalPrincipalPaid() != null) totalPrincipal = totalPrincipal.add(summary.getTotalPrincipalPaid());
            if (summary.getTotalInterestPaid() != null) totalInterest = totalInterest.add(summary.getTotalInterestPaid());
            if (summary.getTotalFeesPaid() != null) totalFees = totalFees.add(summary.getTotalFeesPaid());
            if (summary.getOutstandingLateFees() != null) totalOutstandingLateFees = totalOutstandingLateFees.add(summary.getOutstandingLateFees());
            if (summary.getPaymentCount() != null) totalPaymentCount += summary.getPaymentCount();
        }

        metrics.put("totalCollected", totalCollected);
        metrics.put("totalPrincipalPaid", totalPrincipal);
        metrics.put("totalInterestPaid", totalInterest);
        metrics.put("totalFeesPaid", totalFees);
        metrics.put("outstandingLateFees", totalOutstandingLateFees);
        metrics.put("totalPaymentCount", totalPaymentCount);
        metrics.put("activeLoanCount", summaries.size());

        return metrics;
    }

    /**
     * Payment status breakdown for reporting.
     * Replaces LoanService.getPaymentStatusBreakdown() direct SQL.
     */
    public Map<String, Long> getPaymentStatusBreakdown() {
        Map<String, Long> breakdown = new HashMap<>();
        for (PaymentStatus status : PaymentStatus.values()) {
            long count = paymentRepository.findByStatus(status).size();
            if (count > 0) {
                breakdown.put(status.name(), count);
            }
        }
        return breakdown;
    }
}
