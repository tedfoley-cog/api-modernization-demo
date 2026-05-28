package com.acme.payment.command;

import com.acme.payment.event.EventPublisher;
import com.acme.payment.event.LateFeesAssessed;
import com.acme.payment.event.PaymentAllocated;
import com.acme.payment.event.PaymentProcessed;
import com.acme.payment.event.PaymentReceived;
import com.acme.payment.model.Payment;
import com.acme.payment.model.PaymentMethod;
import com.acme.payment.model.PaymentStatus;
import com.acme.payment.repository.PaymentRepository;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * CQRS write side: handles payment commands and publishes domain events.
 * All state mutations go through this handler — no direct repository access from controllers.
 */
@Service
public class PaymentCommandHandler {

    private static final BigDecimal LATE_FEE_FLAT = new BigDecimal("25.00");
    private static final BigDecimal LATE_FEE_PCT = new BigDecimal("5.00");
    private static final BigDecimal MAX_LATE_FEE = new BigDecimal("50.00");

    private final PaymentRepository paymentRepository;
    private final EventPublisher eventPublisher;
    private final ApplicationContext applicationContext;

    public PaymentCommandHandler(PaymentRepository paymentRepository, EventPublisher eventPublisher,
                                 ApplicationContext applicationContext) {
        this.paymentRepository = paymentRepository;
        this.eventPublisher = eventPublisher;
        this.applicationContext = applicationContext;
    }

    @Transactional
    public Payment handle(SubmitPaymentCommand cmd) {
        Payment payment = new Payment();
        payment.setLoanId(cmd.getLoanId());
        payment.setPaymentAmount(cmd.getPaymentAmount());
        payment.setPaymentMethod(PaymentMethod.valueOf(cmd.getPaymentMethod()));
        payment.setAchRoutingNumber(cmd.getAchRoutingNumber());
        payment.setAchAccountNumber(cmd.getAchAccountNumber());
        payment.setConfirmationNumber("PMT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setPaymentDate(new Date());

        Payment saved = paymentRepository.save(payment);

        // Publish PaymentReceived event — downstream services react asynchronously
        eventPublisher.publish(new PaymentReceived(
                saved.getId(), saved.getLoanId(), saved.getPaymentAmount(),
                saved.getPaymentMethod().name(), saved.getConfirmationNumber()));

        // Allocate payment
        allocatePayment(saved);
        paymentRepository.save(saved);

        // Publish PaymentAllocated event
        eventPublisher.publish(new PaymentAllocated(
                saved.getId(), saved.getLoanId(),
                saved.getPrincipalAmount(), saved.getInterestAmount(), saved.getFeeAmount()));

        // Process ACH payments immediately
        if (saved.getPaymentMethod() == PaymentMethod.ACH) {
            processAchPayment(saved);
        }

        return saved;
    }

    public Map<String, Object> handle(ProcessBatchCommand cmd) {
        PaymentCommandHandler proxy = applicationContext.getBean(PaymentCommandHandler.class);
        Map<String, Object> results = new HashMap<>();
        int processed = 0;
        int failed = 0;

        for (SubmitPaymentCommand paymentCmd : cmd.getPayments()) {
            try {
                proxy.handle(paymentCmd);
                processed++;
            } catch (Exception e) {
                failed++;
            }
        }

        results.put("totalSubmitted", cmd.getPayments().size());
        results.put("processed", processed);
        results.put("failed", failed);
        results.put("batchDate", new Date());
        return results;
    }

    @Transactional
    public Payment handle(AssessLateFeeCommand cmd) {
        BigDecimal lateFee;
        if (cmd.getDaysPastDue() <= 30) {
            lateFee = LATE_FEE_FLAT;
        } else {
            lateFee = cmd.getCurrentBalance()
                    .multiply(LATE_FEE_PCT)
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            if (lateFee.compareTo(MAX_LATE_FEE) > 0) {
                lateFee = MAX_LATE_FEE;
            }
        }

        Payment feePayment = new Payment();
        feePayment.setLoanId(cmd.getLoanId());
        feePayment.setPaymentAmount(BigDecimal.ZERO);
        feePayment.setLateFee(lateFee);
        feePayment.setStatus(PaymentStatus.PENDING);
        feePayment.setPaymentDate(new Date());
        feePayment.setConfirmationNumber("FEE-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());

        Payment saved = paymentRepository.save(feePayment);

        // Publish LateFeesAssessed event
        eventPublisher.publish(new LateFeesAssessed(
                cmd.getLoanId(), lateFee, cmd.getDaysPastDue(), new Date()));

        return saved;
    }

    private void allocatePayment(Payment payment) {
        BigDecimal totalAmount = payment.getPaymentAmount();

        BigDecimal feesPortion = BigDecimal.ZERO;
        List<Payment> pendingFees = paymentRepository.findByLoanIdAndStatus(
                payment.getLoanId(), PaymentStatus.PENDING);
        for (Payment fee : pendingFees) {
            if (fee.getLateFee() != null && fee.getLateFee().compareTo(BigDecimal.ZERO) > 0) {
                feesPortion = feesPortion.add(fee.getLateFee());
            }
        }

        BigDecimal remaining = totalAmount;
        if (feesPortion.compareTo(remaining) > 0) {
            feesPortion = remaining;
        }
        remaining = remaining.subtract(feesPortion);

        // Mark collected fee records as COMPLETED to prevent duplicate charging
        BigDecimal feeBudget = feesPortion;
        for (Payment fee : pendingFees) {
            if (feeBudget.compareTo(BigDecimal.ZERO) <= 0) break;
            if (fee.getLateFee() != null && fee.getLateFee().compareTo(BigDecimal.ZERO) > 0) {
                feeBudget = feeBudget.subtract(fee.getLateFee());
                fee.setStatus(PaymentStatus.COMPLETED);
                paymentRepository.save(fee);
            }
        }

        BigDecimal interestPortion = remaining.multiply(new BigDecimal("0.05"))
                .setScale(2, RoundingMode.HALF_UP);
        if (interestPortion.compareTo(remaining) > 0) {
            interestPortion = remaining;
        }
        remaining = remaining.subtract(interestPortion);

        BigDecimal principalPortion = remaining;

        payment.setFeeAmount(feesPortion);
        payment.setInterestAmount(interestPortion);
        payment.setPrincipalAmount(principalPortion);
    }

    private void processAchPayment(Payment payment) {
        payment.setStatus(PaymentStatus.PROCESSING);
        paymentRepository.save(payment);

        if (payment.getAchRoutingNumber() == null || payment.getAchRoutingNumber().length() != 9) {
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            eventPublisher.publish(new PaymentProcessed(
                    payment.getId(), payment.getLoanId(), new Date(),
                    payment.getStatus().name(), payment.getPrincipalAmount(),
                    paymentRepository.sumCompletedPayments(payment.getLoanId())));
            return;
        }

        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setProcessedDate(new Date());
        paymentRepository.save(payment);

        BigDecimal totalPaid = paymentRepository.sumCompletedPayments(payment.getLoanId());
        eventPublisher.publish(new PaymentProcessed(
                payment.getId(), payment.getLoanId(), payment.getProcessedDate(),
                payment.getStatus().name(), payment.getPrincipalAmount(), totalPaid));
    }
}
