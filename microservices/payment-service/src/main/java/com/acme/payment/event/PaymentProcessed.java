package com.acme.payment.event;

import java.math.BigDecimal;
import java.util.Date;

/**
 * Published when an ACH/EFT payment completes successfully.
 * Replaces synchronous account balance and loan status updates
 * in monolith PaymentService.processAchPayment().
 */
public class PaymentProcessed extends DomainEvent {

    private final Long paymentId;
    private final Long loanId;
    private final Date processedDate;
    private final String status;
    private final BigDecimal principalApplied;
    private final BigDecimal totalPaid;

    public PaymentProcessed(Long paymentId, Long loanId, Date processedDate,
                            String status, BigDecimal principalApplied, BigDecimal totalPaid) {
        super("PaymentProcessed");
        this.paymentId = paymentId;
        this.loanId = loanId;
        this.processedDate = processedDate;
        this.status = status;
        this.principalApplied = principalApplied;
        this.totalPaid = totalPaid;
    }

    public Long getPaymentId() { return paymentId; }
    public Long getLoanId() { return loanId; }
    public Date getProcessedDate() { return processedDate; }
    public String getStatus() { return status; }
    public BigDecimal getPrincipalApplied() { return principalApplied; }
    public BigDecimal getTotalPaid() { return totalPaid; }
}
