package com.acme.payment.query;

import com.acme.payment.domain.Payment;
import com.acme.payment.domain.PaymentMethod;
import com.acme.payment.domain.PaymentStatus;

import java.math.BigDecimal;
import java.util.Date;

/**
 * Read model for the query side of the CQRS split. A flat, immutable projection of a
 * {@link Payment} that is safe to expose to API clients without leaking the JPA entity.
 */
public final class PaymentView {

    private final Long id;
    private final Long loanId;
    private final BigDecimal paymentAmount;
    private final BigDecimal principalAmount;
    private final BigDecimal interestAmount;
    private final BigDecimal feeAmount;
    private final BigDecimal lateFee;
    private final PaymentMethod paymentMethod;
    private final PaymentStatus status;
    private final String confirmationNumber;
    private final Date paymentDate;
    private final Date processedDate;

    private PaymentView(Payment p) {
        this.id = p.getId();
        this.loanId = p.getLoanId();
        this.paymentAmount = p.getPaymentAmount();
        this.principalAmount = p.getPrincipalAmount();
        this.interestAmount = p.getInterestAmount();
        this.feeAmount = p.getFeeAmount();
        this.lateFee = p.getLateFee();
        this.paymentMethod = p.getPaymentMethod();
        this.status = p.getStatus();
        this.confirmationNumber = p.getConfirmationNumber();
        this.paymentDate = p.getPaymentDate();
        this.processedDate = p.getProcessedDate();
    }

    public static PaymentView from(Payment payment) {
        return new PaymentView(payment);
    }

    public Long getId() { return id; }
    public Long getLoanId() { return loanId; }
    public BigDecimal getPaymentAmount() { return paymentAmount; }
    public BigDecimal getPrincipalAmount() { return principalAmount; }
    public BigDecimal getInterestAmount() { return interestAmount; }
    public BigDecimal getFeeAmount() { return feeAmount; }
    public BigDecimal getLateFee() { return lateFee; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public PaymentStatus getStatus() { return status; }
    public String getConfirmationNumber() { return confirmationNumber; }
    public Date getPaymentDate() { return paymentDate; }
    public Date getProcessedDate() { return processedDate; }
}
