package com.acme.payment.service;

import com.acme.payment.domain.event.LateFeeAssessed;
import com.acme.payment.domain.event.PaymentCompleted;
import com.acme.payment.domain.model.Payment;
import com.acme.payment.domain.model.PaymentSummary;
import com.acme.payment.domain.repository.PaymentRepository;
import com.acme.payment.domain.repository.PaymentSummaryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * CQRS projection: updates the payment_summary read model in response to domain events.
 * This replaces the monolith's direct SQL queries across shared tables.
 */
@Component
public class PaymentSummaryProjection {

    private static final Logger log = LoggerFactory.getLogger(PaymentSummaryProjection.class);

    private final PaymentSummaryRepository summaryRepository;
    private final PaymentRepository paymentRepository;

    public PaymentSummaryProjection(PaymentSummaryRepository summaryRepository,
                                    PaymentRepository paymentRepository) {
        this.summaryRepository = summaryRepository;
        this.paymentRepository = paymentRepository;
    }

    @EventListener
    @Transactional
    public void onPaymentCompleted(PaymentCompleted event) {
        PaymentSummary summary = summaryRepository.findById(event.getLoanId())
                .orElseGet(() -> {
                    PaymentSummary s = new PaymentSummary();
                    s.setLoanId(event.getLoanId());
                    s.setTotalPaid(BigDecimal.ZERO);
                    s.setTotalPrincipalPaid(BigDecimal.ZERO);
                    s.setTotalInterestPaid(BigDecimal.ZERO);
                    s.setTotalFeesPaid(BigDecimal.ZERO);
                    s.setOutstandingLateFees(BigDecimal.ZERO);
                    s.setPaymentCount(0);
                    return s;
                });

        // Find the payment to get detailed allocation
        Optional<Payment> paymentOpt = paymentRepository.findById(event.getPaymentId());
        if (paymentOpt.isPresent()) {
            Payment payment = paymentOpt.get();
            summary.setTotalPaid(summary.getTotalPaid().add(
                    payment.getPaymentAmount() != null ? payment.getPaymentAmount() : BigDecimal.ZERO));
            summary.setTotalPrincipalPaid(summary.getTotalPrincipalPaid().add(
                    payment.getPrincipalAmount() != null ? payment.getPrincipalAmount() : BigDecimal.ZERO));
            summary.setTotalInterestPaid(summary.getTotalInterestPaid().add(
                    payment.getInterestAmount() != null ? payment.getInterestAmount() : BigDecimal.ZERO));
            summary.setTotalFeesPaid(summary.getTotalFeesPaid().add(
                    payment.getFeeAmount() != null ? payment.getFeeAmount() : BigDecimal.ZERO));
            summary.setPaymentCount(summary.getPaymentCount() + 1);
            summary.setLastPaymentDate(payment.getPaymentDate());
            summary.setLastPaymentAmount(payment.getPaymentAmount());
        }

        summaryRepository.save(summary);
        log.info("Projection updated: loanId={}, totalPaid={}, paymentCount={}",
                event.getLoanId(), summary.getTotalPaid(), summary.getPaymentCount());
    }

    @EventListener
    @Transactional
    public void onLateFeeAssessed(LateFeeAssessed event) {
        PaymentSummary summary = summaryRepository.findById(event.getLoanId())
                .orElseGet(() -> {
                    PaymentSummary s = new PaymentSummary();
                    s.setLoanId(event.getLoanId());
                    s.setTotalPaid(BigDecimal.ZERO);
                    s.setTotalPrincipalPaid(BigDecimal.ZERO);
                    s.setTotalInterestPaid(BigDecimal.ZERO);
                    s.setTotalFeesPaid(BigDecimal.ZERO);
                    s.setOutstandingLateFees(BigDecimal.ZERO);
                    s.setPaymentCount(0);
                    return s;
                });

        summary.setOutstandingLateFees(summary.getOutstandingLateFees().add(event.getFeeAmount()));
        summaryRepository.save(summary);
        log.info("Projection updated: loanId={}, outstandingLateFees={}",
                event.getLoanId(), summary.getOutstandingLateFees());
    }
}
