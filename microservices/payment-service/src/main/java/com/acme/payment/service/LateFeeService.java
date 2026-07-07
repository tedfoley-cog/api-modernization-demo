package com.acme.payment.service;

import com.acme.payment.config.EventBusConfig;
import com.acme.payment.domain.event.LateFeeAssessed;
import com.acme.payment.domain.model.Payment;
import com.acme.payment.domain.model.PaymentStatus;
import com.acme.payment.domain.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.UUID;

/**
 * Late fee assessment logic extracted from the monolith.
 * Externalized fee schedule (previously hardcoded in PaymentService).
 */
@Service
public class LateFeeService {

    private static final Logger log = LoggerFactory.getLogger(LateFeeService.class);

    private final PaymentRepository paymentRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final EventBusConfig.EventStore eventStore;

    // Externalized configuration — in production, these would come from a config service
    private static final BigDecimal LATE_FEE_FLAT = new BigDecimal("25.00");
    private static final BigDecimal LATE_FEE_PCT = new BigDecimal("5.00");
    private static final BigDecimal MAX_LATE_FEE = new BigDecimal("50.00");

    public LateFeeService(PaymentRepository paymentRepository,
                          ApplicationEventPublisher eventPublisher,
                          EventBusConfig.EventStore eventStore) {
        this.paymentRepository = paymentRepository;
        this.eventPublisher = eventPublisher;
        this.eventStore = eventStore;
    }

    @Transactional
    public Payment assessLateFee(Long loanId, int daysPastDue, BigDecimal currentBalance) {
        BigDecimal lateFee;
        if (daysPastDue <= 30) {
            lateFee = LATE_FEE_FLAT;
        } else {
            lateFee = currentBalance
                    .multiply(LATE_FEE_PCT)
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            if (lateFee.compareTo(MAX_LATE_FEE) > 0) {
                lateFee = MAX_LATE_FEE;
            }
        }

        String confirmationNumber = "FEE-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Payment feePayment = new Payment();
        feePayment.setLoanId(loanId);
        feePayment.setPaymentAmount(BigDecimal.ZERO);
        feePayment.setLateFee(lateFee);
        feePayment.setStatus(PaymentStatus.PENDING);
        feePayment.setPaymentDate(new Date());
        feePayment.setConfirmationNumber(confirmationNumber);
        Payment saved = paymentRepository.save(feePayment);

        LateFeeAssessed event = new LateFeeAssessed(loanId, lateFee, daysPastDue, confirmationNumber);
        eventStore.record(event);
        eventPublisher.publishEvent(event);
        log.info("Published LateFeeAssessed: loanId={}, fee={}, daysPastDue={}",
                loanId, lateFee, daysPastDue);

        return saved;
    }
}
