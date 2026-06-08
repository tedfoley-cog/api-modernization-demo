package com.acme.payment.domain;

import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

/**
 * Payment aggregate root for the Payment Processing bounded context.
 *
 * <p>Unlike the monolith's anemic {@code Payment} row, this aggregate encapsulates its
 * own state transitions ({@link #allocate}, {@link #beginProcessing}, {@link #complete},
 * {@link #fail}). It performs allocation using only the loan context handed to it at the
 * boundary — it never reaches into the loan or account databases.
 */
@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "loan_id", nullable = false)
    private Long loanId;

    @Column(name = "payment_amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal paymentAmount;

    @Column(name = "late_fee", precision = 12, scale = 2)
    private BigDecimal lateFee = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method")
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PaymentStatus status;

    @Column(name = "confirmation_number", nullable = false, unique = true)
    private String confirmationNumber;

    @Column(name = "ach_routing_number")
    private String achRoutingNumber;

    @Embedded
    private PaymentAllocation allocation = PaymentAllocation.empty();

    @Column(name = "received_at")
    private Instant receivedAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    protected Payment() {
    }

    private Payment(Long loanId, BigDecimal paymentAmount, PaymentMethod method, String achRoutingNumber) {
        this.loanId = loanId;
        this.paymentAmount = paymentAmount;
        this.paymentMethod = method;
        this.achRoutingNumber = achRoutingNumber;
        this.status = PaymentStatus.PENDING;
        this.confirmationNumber = "PMT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        this.receivedAt = Instant.now();
    }

    /** Factory for a borrower-submitted payment. */
    public static Payment received(Long loanId, BigDecimal paymentAmount, PaymentMethod method,
                                   String achRoutingNumber) {
        if (loanId == null) {
            throw new IllegalArgumentException("loanId is required");
        }
        if (paymentAmount == null || paymentAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("paymentAmount must be positive");
        }
        return new Payment(loanId, paymentAmount, method, achRoutingNumber);
    }

    /** Factory for a system-generated late-fee charge. */
    public static Payment lateFeeCharge(Long loanId, BigDecimal feeAmount) {
        Payment p = new Payment(loanId, BigDecimal.ZERO, null, null);
        p.lateFee = feeAmount;
        p.confirmationNumber = "FEE-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        p.allocation = new PaymentAllocation(feeAmount, BigDecimal.ZERO, BigDecimal.ZERO);
        // A late fee is assessed immediately — it is not awaiting processing, so it must not
        // surface in the pending-payments queue alongside CHECK/WIRE submissions.
        p.status = PaymentStatus.COMPLETED;
        p.processedAt = Instant.now();
        return p;
    }

    /**
     * Split the payment into fees, interest and principal using the loan context provided
     * at the boundary. No cross-domain repository lookups — the allocation order (fees,
     * then interest, then principal) is the same business rule the monolith used.
     */
    public PaymentAllocation allocate(BigDecimal outstandingBalance, BigDecimal annualInterestRate,
                                      BigDecimal outstandingFees) {
        BigDecimal remaining = this.paymentAmount;

        BigDecimal fees = outstandingFees == null ? BigDecimal.ZERO : outstandingFees;
        if (fees.compareTo(remaining) > 0) {
            fees = remaining;
        }
        remaining = remaining.subtract(fees);

        BigDecimal interest = BigDecimal.ZERO;
        if (outstandingBalance != null && annualInterestRate != null) {
            BigDecimal monthlyRate = annualInterestRate.divide(new BigDecimal("1200"), 10, RoundingMode.HALF_UP);
            interest = outstandingBalance.multiply(monthlyRate).setScale(2, RoundingMode.HALF_UP);
        }
        if (interest.compareTo(remaining) > 0) {
            interest = remaining;
        }
        remaining = remaining.subtract(interest);

        BigDecimal principal = remaining;
        this.allocation = new PaymentAllocation(fees, interest, principal);
        return this.allocation;
    }

    public void beginProcessing() {
        requireStatus(PaymentStatus.PENDING);
        this.status = PaymentStatus.PROCESSING;
    }

    public void complete() {
        requireStatus(PaymentStatus.PROCESSING);
        this.status = PaymentStatus.COMPLETED;
        this.processedAt = Instant.now();
    }

    public void fail() {
        this.status = PaymentStatus.FAILED;
        this.processedAt = Instant.now();
    }

    public boolean isAch() {
        return this.paymentMethod == PaymentMethod.ACH;
    }

    /** Electronic methods (ACH/EFT) clear straight through; CHECK/WIRE settle out of band. */
    public boolean isElectronic() {
        return this.paymentMethod == PaymentMethod.ACH || this.paymentMethod == PaymentMethod.EFT;
    }

    public boolean hasValidAchRouting() {
        return this.achRoutingNumber != null && this.achRoutingNumber.length() == 9;
    }

    private void requireStatus(PaymentStatus expected) {
        if (this.status != expected) {
            throw new IllegalStateException(
                    "Payment " + id + " must be " + expected + " but was " + status);
        }
    }

    public Long getId() {
        return id;
    }

    public Long getLoanId() {
        return loanId;
    }

    public BigDecimal getPaymentAmount() {
        return paymentAmount;
    }

    public BigDecimal getLateFee() {
        return lateFee;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public String getConfirmationNumber() {
        return confirmationNumber;
    }

    public String getAchRoutingNumber() {
        return achRoutingNumber;
    }

    public PaymentAllocation getAllocation() {
        return allocation;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }
}
