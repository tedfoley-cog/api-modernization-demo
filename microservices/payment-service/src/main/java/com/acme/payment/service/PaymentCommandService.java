package com.acme.payment.service;

import com.acme.payment.config.EventBusConfig;
import com.acme.payment.domain.event.PaymentCompleted;
import com.acme.payment.domain.event.PaymentFailed;
import com.acme.payment.domain.event.PaymentReceived;
import com.acme.payment.domain.model.Payment;
import com.acme.payment.domain.model.PaymentAllocation;
import com.acme.payment.domain.model.PaymentMethod;
import com.acme.payment.domain.model.PaymentStatus;
import com.acme.payment.domain.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * CQRS write side: handles payment submissions and processing.
 * Publishes domain events instead of synchronously calling other services.
 */
@Service
public class PaymentCommandService {

    private static final Logger log = LoggerFactory.getLogger(PaymentCommandService.class);

    private final PaymentRepository paymentRepository;
    private final PaymentCalculationService calculationService;
    private final ApplicationEventPublisher eventPublisher;
    private final EventBusConfig.EventStore eventStore;
    private final TransactionTemplate batchTransactionTemplate;

    public PaymentCommandService(PaymentRepository paymentRepository,
                                 PaymentCalculationService calculationService,
                                 ApplicationEventPublisher eventPublisher,
                                 EventBusConfig.EventStore eventStore,
                                 PlatformTransactionManager transactionManager) {
        this.paymentRepository = paymentRepository;
        this.calculationService = calculationService;
        this.eventPublisher = eventPublisher;
        this.eventStore = eventStore;
        this.batchTransactionTemplate = new TransactionTemplate(transactionManager);
        this.batchTransactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Transactional
    public Payment submitPayment(Payment payment, BigDecimal currentBalance,
                                 BigDecimal annualRate) {
        payment.setConfirmationNumber("PMT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setPaymentDate(new Date());

        // Allocate payment: fees -> interest -> principal
        BigDecimal outstandingFees = calculateOutstandingFees(payment.getLoanId());
        BigDecimal monthlyRate = annualRate != null
                ? annualRate.divide(new BigDecimal("1200"), 10, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        BigDecimal interestPortion = currentBalance != null
                ? currentBalance.multiply(monthlyRate).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        PaymentAllocation allocation = calculationService.allocatePayment(
                payment.getPaymentAmount(), outstandingFees, interestPortion);

        payment.setFeeAmount(allocation.getFeeAmount());
        payment.setInterestAmount(allocation.getInterestAmount());
        payment.setPrincipalAmount(allocation.getPrincipalAmount());

        Payment saved = paymentRepository.saveAndFlush(payment);

        // Publish PaymentReceived event
        PaymentReceived event = new PaymentReceived(
                saved.getLoanId(), saved.getId(), saved.getPaymentAmount(),
                saved.getPrincipalAmount(), saved.getInterestAmount(),
                saved.getFeeAmount(), saved.getConfirmationNumber());
        eventStore.record(event);
        eventPublisher.publishEvent(event);
        log.info("Published PaymentReceived: loanId={}, amount={}, confirmation={}",
                saved.getLoanId(), saved.getPaymentAmount(), saved.getConfirmationNumber());

        // Process immediately if ACH
        if (saved.getPaymentMethod() == PaymentMethod.ACH) {
            processAchPayment(saved);
        }

        return saved;
    }

    @Transactional
    public void processAchPayment(Payment payment) {
        payment.setStatus(PaymentStatus.PROCESSING);
        paymentRepository.save(payment);

        // Validate routing number
        if (payment.getAchRoutingNumber() == null || payment.getAchRoutingNumber().length() != 9) {
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);

            PaymentFailed failedEvent = new PaymentFailed(
                    payment.getLoanId(), payment.getId(),
                    "Invalid ACH routing number", payment.getConfirmationNumber());
            eventStore.record(failedEvent);
            eventPublisher.publishEvent(failedEvent);
            log.warn("Published PaymentFailed: loanId={}, reason=Invalid ACH routing number",
                    payment.getLoanId());
            return;
        }

        // Mark as completed
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setProcessedDate(new Date());
        paymentRepository.save(payment);

        // Publish PaymentCompleted — replaces synchronous updateAccountBalance + updateLoanAfterPayment
        BigDecimal totalPaid = paymentRepository.sumCompletedPayments(payment.getLoanId());
        PaymentCompleted completedEvent = new PaymentCompleted(
                payment.getLoanId(), payment.getId(),
                payment.getPrincipalAmount(), totalPaid, payment.getConfirmationNumber());
        eventStore.record(completedEvent);
        eventPublisher.publishEvent(completedEvent);
        log.info("Published PaymentCompleted: loanId={}, totalPaidToDate={}",
                payment.getLoanId(), totalPaid);
    }

    public Map<String, Object> processBatchPayments(List<Payment> payments,
                                                     BigDecimal defaultBalance,
                                                     BigDecimal defaultRate) {
        Map<String, Object> results = new HashMap<>();
        int processed = 0;
        int failed = 0;

        for (Payment payment : payments) {
            try {
                batchTransactionTemplate.execute(status -> {
                    submitPayment(payment, defaultBalance, defaultRate);
                    return null;
                });
                processed++;
            } catch (Exception e) {
                log.error("Batch payment failed for loanId={}: {}", payment.getLoanId(), e.getMessage());
                failed++;
            }
        }

        results.put("totalSubmitted", payments.size());
        results.put("processed", processed);
        results.put("failed", failed);
        results.put("batchDate", new Date());
        return results;
    }

    private BigDecimal calculateOutstandingFees(Long loanId) {
        BigDecimal fees = BigDecimal.ZERO;
        List<Payment> pendingFees = paymentRepository.findByLoanIdAndStatus(
                loanId, PaymentStatus.PENDING);
        for (Payment fee : pendingFees) {
            if (fee.getLateFee() != null && fee.getLateFee().compareTo(BigDecimal.ZERO) > 0) {
                fees = fees.add(fee.getLateFee());
            }
        }
        return fees;
    }
}
