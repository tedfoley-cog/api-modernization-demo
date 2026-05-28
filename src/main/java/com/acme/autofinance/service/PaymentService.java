package com.acme.autofinance.service;

import com.acme.autofinance.model.Account;
import com.acme.autofinance.model.LoanApplication;
import com.acme.autofinance.model.Payment;
import com.acme.autofinance.model.PaymentMethod;
import com.acme.autofinance.model.PaymentStatus;
import com.acme.autofinance.repository.AccountRepository;
import com.acme.autofinance.repository.LoanRepository;
import com.acme.autofinance.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Payment processing service — synchronous coupling to loan and account domains.
 * Every payment submission triggers direct updates to loan balance and account status.
 */
@Service
public class PaymentService {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private AccountService accountService;

    // Hardcoded late fee schedule — should be externalized configuration
    private static final BigDecimal LATE_FEE_FLAT = new BigDecimal("25.00");
    private static final BigDecimal LATE_FEE_PCT = new BigDecimal("5.00");
    private static final BigDecimal MAX_LATE_FEE = new BigDecimal("50.00");

    @Transactional
    public Payment submitPayment(Payment payment) {
        payment.setConfirmationNumber("PMT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setPaymentDate(new Date());

        // Validate the loan exists — synchronous cross-domain check
        LoanApplication loan = loanRepository.findById(payment.getLoanId())
                .orElseThrow(() -> new RuntimeException("Loan not found: " + payment.getLoanId()));

        // Allocate payment: principal vs interest vs fees — inline business logic
        allocatePayment(payment, loan);

        Payment saved = paymentRepository.save(payment);

        // Process immediately if ACH — synchronous processing
        if (payment.getPaymentMethod() == PaymentMethod.ACH) {
            processAchPayment(saved);
        }

        return saved;
    }

    private void allocatePayment(Payment payment, LoanApplication loan) {
        BigDecimal totalAmount = payment.getPaymentAmount();
        BigDecimal monthlyRate = BigDecimal.ZERO;
        if (loan.getInterestRate() != null) {
            monthlyRate = loan.getInterestRate()
                    .divide(new BigDecimal("1200"), 10, RoundingMode.HALF_UP);
        }

        // Interest portion: current balance * monthly rate
        Optional<Account> accountOpt = accountRepository.findByLoanId(loan.getId());
        BigDecimal currentBalance = accountOpt.isPresent()
                ? accountOpt.get().getCurrentBalance()
                : loan.getApprovedAmount();

        BigDecimal interestPortion = BigDecimal.ZERO;
        if (currentBalance != null) {
            interestPortion = currentBalance.multiply(monthlyRate).setScale(2, RoundingMode.HALF_UP);
        }

        // Late fees first, then interest, then principal — hardcoded allocation order
        BigDecimal feesPortion = BigDecimal.ZERO;
        BigDecimal remaining = totalAmount;

        // Apply any outstanding late fees first
        List<Payment> pendingFees = paymentRepository.findByLoanIdAndStatus(
                loan.getId(), PaymentStatus.PENDING);
        for (Payment fee : pendingFees) {
            if (fee.getLateFee() != null && fee.getLateFee().compareTo(BigDecimal.ZERO) > 0) {
                feesPortion = feesPortion.add(fee.getLateFee());
            }
        }

        if (feesPortion.compareTo(remaining) > 0) {
            feesPortion = remaining;
        }
        remaining = remaining.subtract(feesPortion);

        // Then interest
        if (interestPortion.compareTo(remaining) > 0) {
            interestPortion = remaining;
        }
        remaining = remaining.subtract(interestPortion);

        // Rest goes to principal
        BigDecimal principalPortion = remaining;

        payment.setFeeAmount(feesPortion);
        payment.setInterestAmount(interestPortion);
        payment.setPrincipalAmount(principalPortion);
    }

    @Transactional
    public void processAchPayment(Payment payment) {
        // Simulate ACH processing — in production this calls external ACH processor
        payment.setStatus(PaymentStatus.PROCESSING);
        paymentRepository.save(payment);

        // Validate routing/account numbers — inline
        if (payment.getAchRoutingNumber() == null || payment.getAchRoutingNumber().length() != 9) {
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            return;
        }

        // Mark as completed
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setProcessedDate(new Date());
        paymentRepository.save(payment);

        // Synchronously update the account balance — tight coupling
        updateAccountBalance(payment);

        // Synchronously update loan status — cross-domain side effect
        updateLoanAfterPayment(payment);
    }

    private void updateAccountBalance(Payment payment) {
        Optional<Account> accountOpt = accountRepository.findByLoanId(payment.getLoanId());
        if (accountOpt.isPresent()) {
            Account account = accountOpt.get();
            if (account.getCurrentBalance() != null && payment.getPrincipalAmount() != null) {
                BigDecimal newBalance = account.getCurrentBalance().subtract(payment.getPrincipalAmount());
                if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
                    newBalance = BigDecimal.ZERO;
                }
                account.setCurrentBalance(newBalance);
                account.setLastPaymentDate(new Date());
                account.setDaysPastDue(0);
                accountRepository.save(account);
            }
        }
    }

    private void updateLoanAfterPayment(Payment payment) {
        Optional<LoanApplication> loanOpt = loanRepository.findById(payment.getLoanId());
        if (loanOpt.isPresent()) {
            LoanApplication loan = loanOpt.get();
            // Check if loan is paid off
            BigDecimal totalPaid = paymentRepository.sumCompletedPayments(loan.getId());
            if (totalPaid != null && loan.getApprovedAmount() != null) {
                if (totalPaid.compareTo(loan.getApprovedAmount()) >= 0) {
                    loan.setStatus(com.acme.autofinance.model.LoanStatus.PAID_OFF);
                    loanRepository.save(loan);
                    // Also close the account — synchronous cross-domain update
                    accountService.closeAccount(loan.getId());
                }
            }
        }
    }

    public void assessLateFee(Long loanId, int daysPastDue) {
        Optional<Account> accountOpt = accountRepository.findByLoanId(loanId);
        if (!accountOpt.isPresent()) return;

        Account account = accountOpt.get();

        // Calculate late fee — hardcoded business rules
        BigDecimal lateFee;
        if (daysPastDue <= 30) {
            lateFee = LATE_FEE_FLAT;
        } else {
            // Percentage-based for >30 days
            lateFee = account.getCurrentBalance()
                    .multiply(LATE_FEE_PCT)
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            if (lateFee.compareTo(MAX_LATE_FEE) > 0) {
                lateFee = MAX_LATE_FEE;
            }
        }

        Payment feePayment = new Payment();
        feePayment.setLoanId(loanId);
        feePayment.setPaymentAmount(BigDecimal.ZERO);
        feePayment.setLateFee(lateFee);
        feePayment.setStatus(PaymentStatus.PENDING);
        feePayment.setPaymentDate(new Date());
        feePayment.setConfirmationNumber("FEE-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        paymentRepository.save(feePayment);
    }

    public List<Payment> getPaymentHistory(Long loanId) {
        return paymentRepository.findByLoanId(loanId);
    }

    @Transactional
    public Map<String, Object> processBatchPayments(List<Payment> payments) {
        Map<String, Object> results = new HashMap<>();
        int processed = 0;
        int failed = 0;

        for (Payment payment : payments) {
            try {
                submitPayment(payment);
                processed++;
            } catch (Exception e) {
                failed++;
            }
        }

        results.put("totalSubmitted", payments.size());
        results.put("processed", processed);
        results.put("failed", failed);
        results.put("batchDate", new Date());
        return results;
    }

    public List<Payment> getPendingPayments() {
        return paymentRepository.findByStatus(PaymentStatus.PENDING);
    }
}
