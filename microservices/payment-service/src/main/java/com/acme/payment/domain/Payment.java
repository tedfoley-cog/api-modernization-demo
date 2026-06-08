package com.acme.payment.domain;

import javax.persistence.Column;
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
 * Rich payment aggregate. Unlike the anemic monolith entity, allocation and
 * late-fee construction live <em>inside</em> the domain object and depend only
 * on values passed in — never on loan/account repositories.
 */
@Entity
@Table(name = "payments")
public class Payment {

    // Hardcoded late fee schedule (matches the legacy monolith rules).
    private static final BigDecimal LATE_FEE_FLAT = new BigDecimal("25.00");
    private static final BigDecimal LATE_FEE_PCT = new BigDecimal("5.00");
    private static final BigDecimal MAX_LATE_FEE = new BigDecimal("50.00");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "loan_id", nullable = false)
    private Long loanId;

    @Column(name = "payment_amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal paymentAmount;

    @Column(name = "principal_amount", precision = 12, scale = 2)
    private BigDecimal principalAmount;

    @Column(name = "interest_amount", precision = 12, scale = 2)
    private BigDecimal interestAmount;

    @Column(name = "fee_amount", precision = 12, scale = 2)
    private BigDecimal feeAmount;

    @Column(name = "late_fee", precision = 12, scale = 2)
    private BigDecimal lateFee;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method")
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private PaymentStatus status;

    @Column(name = "confirmation_number")
    private String confirmationNumber;

    @Column(name = "ach_routing_number")
    private String achRoutingNumber;

    @Column(name = "ach_account_number")
    private String achAccountNumber;

    @Column(name = "payment_date")
    private Instant paymentDate;

    @Column(name = "processed_date")
    private Instant processedDate;

    @Column(name = "due_date")
    private Instant dueDate;

    public Payment() {
    }

    /**
     * Factory for a brand-new customer payment. Stamps a confirmation number,
     * PENDING status and the submission timestamp.
     */
    public static Payment submit(Long loanId,
                                 BigDecimal paymentAmount,
                                 PaymentMethod paymentMethod,
                                 String achRoutingNumber,
                                 String achAccountNumber) {
        Payment p = new Payment();
        p.loanId = loanId;
        p.paymentAmount = paymentAmount;
        p.paymentMethod = paymentMethod;
        p.achRoutingNumber = achRoutingNumber;
        p.achAccountNumber = achAccountNumber;
        p.status = PaymentStatus.PENDING;
        p.paymentDate = Instant.now();
        p.confirmationNumber = "PMT-" + shortId();
        return p;
    }

    /**
     * Factory for a late-fee charge. Encapsulates the fee schedule that used to
     * live in the monolith service: flat $25 for &lt;= 30 days past due,
     * otherwise 5% of the outstanding balance capped at $50.
     */
    public static Payment lateFeeCharge(Long loanId, BigDecimal outstandingBalance, int daysPastDue) {
        BigDecimal fee = computeLateFee(outstandingBalance, daysPastDue);
        Payment p = new Payment();
        p.loanId = loanId;
        p.paymentAmount = BigDecimal.ZERO;
        p.lateFee = fee;
        p.feeAmount = fee;
        p.status = PaymentStatus.PENDING;
        p.paymentDate = Instant.now();
        p.confirmationNumber = "FEE-" + shortId();
        return p;
    }

    public static BigDecimal computeLateFee(BigDecimal outstandingBalance, int daysPastDue) {
        if (daysPastDue <= 30) {
            return LATE_FEE_FLAT;
        }
        BigDecimal base = outstandingBalance == null ? BigDecimal.ZERO : outstandingBalance;
        BigDecimal fee = base.multiply(LATE_FEE_PCT)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        return fee.compareTo(MAX_LATE_FEE) > 0 ? MAX_LATE_FEE : fee;
    }

    /**
     * Splits {@link #paymentAmount} across outstanding fees, interest and
     * principal (in that priority order) and records the result on this entity.
     *
     * <p>All inputs are supplied by the caller — this method never reaches into
     * loan or account repositories, which is what decouples it from those domains.
     *
     * @param outstandingBalance    current loan balance (drives interest)
     * @param monthlyInterestRate   monthly rate as a fraction (e.g. 0.005 for 6% APR)
     * @param outstandingFees       sum of unpaid late fees to recover first
     */
    public PaymentAllocation allocate(BigDecimal outstandingBalance,
                                      BigDecimal monthlyInterestRate,
                                      BigDecimal outstandingFees) {
        BigDecimal remaining = paymentAmount == null ? BigDecimal.ZERO : paymentAmount;

        BigDecimal interestDue = BigDecimal.ZERO;
        if (outstandingBalance != null && monthlyInterestRate != null) {
            interestDue = outstandingBalance.multiply(monthlyInterestRate).setScale(2, RoundingMode.HALF_UP);
        }

        // Fees first.
        BigDecimal fees = outstandingFees == null ? BigDecimal.ZERO : outstandingFees;
        if (fees.compareTo(remaining) > 0) {
            fees = remaining;
        }
        remaining = remaining.subtract(fees);

        // Then interest.
        BigDecimal interest = interestDue;
        if (interest.compareTo(remaining) > 0) {
            interest = remaining;
        }
        remaining = remaining.subtract(interest);

        // Whatever is left reduces principal.
        BigDecimal principal = remaining;

        PaymentAllocation allocation = new PaymentAllocation(fees, interest, principal);
        this.feeAmount = allocation.getFeeAmount();
        this.interestAmount = allocation.getInterestAmount();
        this.principalAmount = allocation.getPrincipalAmount();
        return allocation;
    }

    public boolean isAch() {
        return paymentMethod == PaymentMethod.ACH;
    }

    /**
     * Performs the ACH settlement state transition on this aggregate. Validates
     * the routing number and marks the payment COMPLETED (or FAILED).
     *
     * @return true if the payment settled successfully
     */
    public boolean settleAch() {
        status = PaymentStatus.PROCESSING;
        if (achRoutingNumber == null || achRoutingNumber.length() != 9) {
            status = PaymentStatus.FAILED;
            return false;
        }
        status = PaymentStatus.COMPLETED;
        processedDate = Instant.now();
        return true;
    }

    private static String shortId() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getLoanId() {
        return loanId;
    }

    public void setLoanId(Long loanId) {
        this.loanId = loanId;
    }

    public BigDecimal getPaymentAmount() {
        return paymentAmount;
    }

    public void setPaymentAmount(BigDecimal paymentAmount) {
        this.paymentAmount = paymentAmount;
    }

    public BigDecimal getPrincipalAmount() {
        return principalAmount;
    }

    public void setPrincipalAmount(BigDecimal principalAmount) {
        this.principalAmount = principalAmount;
    }

    public BigDecimal getInterestAmount() {
        return interestAmount;
    }

    public void setInterestAmount(BigDecimal interestAmount) {
        this.interestAmount = interestAmount;
    }

    public BigDecimal getFeeAmount() {
        return feeAmount;
    }

    public void setFeeAmount(BigDecimal feeAmount) {
        this.feeAmount = feeAmount;
    }

    public BigDecimal getLateFee() {
        return lateFee;
    }

    public void setLateFee(BigDecimal lateFee) {
        this.lateFee = lateFee;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    public String getConfirmationNumber() {
        return confirmationNumber;
    }

    public void setConfirmationNumber(String confirmationNumber) {
        this.confirmationNumber = confirmationNumber;
    }

    public String getAchRoutingNumber() {
        return achRoutingNumber;
    }

    public void setAchRoutingNumber(String achRoutingNumber) {
        this.achRoutingNumber = achRoutingNumber;
    }

    public String getAchAccountNumber() {
        return achAccountNumber;
    }

    public void setAchAccountNumber(String achAccountNumber) {
        this.achAccountNumber = achAccountNumber;
    }

    public Instant getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(Instant paymentDate) {
        this.paymentDate = paymentDate;
    }

    public Instant getProcessedDate() {
        return processedDate;
    }

    public void setProcessedDate(Instant processedDate) {
        this.processedDate = processedDate;
    }

    public Instant getDueDate() {
        return dueDate;
    }

    public void setDueDate(Instant dueDate) {
        this.dueDate = dueDate;
    }
}
