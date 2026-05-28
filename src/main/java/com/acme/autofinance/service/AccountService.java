package com.acme.autofinance.service;

import com.acme.autofinance.model.Account;
import com.acme.autofinance.model.AccountStatus;
import com.acme.autofinance.model.LoanApplication;
import com.acme.autofinance.model.Payment;
import com.acme.autofinance.repository.AccountRepository;
import com.acme.autofinance.repository.LoanRepository;
import com.acme.autofinance.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Account servicing — cross-references loan, payment, and customer data directly.
 * Calculates payoff quotes with complex multi-domain lookups.
 */
@Service
public class AccountService {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    public Account getAccountDetails(Long accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found: " + accountId));
    }

    public Account getByAccountNumber(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("Account not found: " + accountNumber));
    }

    @Transactional
    public Account updateAddress(Long accountId, String newAddress) {
        Account account = getAccountDetails(accountId);
        account.setMailingAddress(newAddress);
        return accountRepository.save(account);
    }

    public Map<String, Object> calculatePayoffQuote(Long accountId) {
        Account account = getAccountDetails(accountId);
        Map<String, Object> quote = new HashMap<>();

        // Cross-domain lookup: get loan details to calculate per-diem interest
        Optional<LoanApplication> loanOpt = loanRepository.findById(account.getLoanId());
        if (!loanOpt.isPresent()) {
            throw new RuntimeException("Associated loan not found for account: " + accountId);
        }

        LoanApplication loan = loanOpt.get();

        // Per-diem interest calculation — inline business logic
        BigDecimal dailyRate = BigDecimal.ZERO;
        if (loan.getInterestRate() != null) {
            dailyRate = loan.getInterestRate()
                    .divide(new BigDecimal("36500"), 10, RoundingMode.HALF_UP);
        }

        BigDecimal perDiem = account.getCurrentBalance() != null
                ? account.getCurrentBalance().multiply(dailyRate).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Payoff good for 10 days
        BigDecimal tenDayInterest = perDiem.multiply(new BigDecimal("10")).setScale(2, RoundingMode.HALF_UP);

        // Cross-domain lookup: get outstanding fees from payment records
        BigDecimal outstandingFees = BigDecimal.ZERO;
        List<Payment> payments = paymentRepository.findByLoanId(account.getLoanId());
        for (Payment payment : payments) {
            if (payment.getLateFee() != null && payment.getLateFee().compareTo(BigDecimal.ZERO) > 0
                    && payment.getPaymentAmount().compareTo(BigDecimal.ZERO) == 0) {
                outstandingFees = outstandingFees.add(payment.getLateFee());
            }
        }

        BigDecimal totalPayoff = (account.getCurrentBalance() != null ? account.getCurrentBalance() : BigDecimal.ZERO)
                .add(tenDayInterest)
                .add(outstandingFees);

        quote.put("accountNumber", account.getAccountNumber());
        quote.put("currentBalance", account.getCurrentBalance());
        quote.put("perDiemInterest", perDiem);
        quote.put("tenDayInterest", tenDayInterest);
        quote.put("outstandingFees", outstandingFees);
        quote.put("totalPayoffAmount", totalPayoff);
        quote.put("goodThroughDate", getDatePlusDays(10));
        quote.put("quoteDate", new Date());

        return quote;
    }

    @Transactional
    public Map<String, Object> processEarlyTermination(Long accountId) {
        Account account = getAccountDetails(accountId);
        Map<String, Object> result = new HashMap<>();

        // Calculate payoff quote
        Map<String, Object> payoff = calculatePayoffQuote(accountId);

        // Update account status
        account.setStatus(AccountStatus.EARLY_TERMINATION);
        accountRepository.save(account);

        // Cross-domain: update loan status
        Optional<LoanApplication> loanOpt = loanRepository.findById(account.getLoanId());
        if (loanOpt.isPresent()) {
            loanOpt.get().setStatus(com.acme.autofinance.model.LoanStatus.PAID_OFF);
            loanRepository.save(loanOpt.get());
        }

        result.put("accountNumber", account.getAccountNumber());
        result.put("terminationDate", new Date());
        result.put("finalPayoffAmount", payoff.get("totalPayoffAmount"));
        result.put("status", "TERMINATED");

        return result;
    }

    @Transactional
    public void activateAccount(Long loanId) {
        Optional<Account> accountOpt = accountRepository.findByLoanId(loanId);
        if (accountOpt.isPresent()) {
            Account account = accountOpt.get();
            account.setStatus(AccountStatus.CURRENT);
            accountRepository.save(account);
        }
    }

    @Transactional
    public void closeAccount(Long loanId) {
        Optional<Account> accountOpt = accountRepository.findByLoanId(loanId);
        if (accountOpt.isPresent()) {
            Account account = accountOpt.get();
            account.setStatus(AccountStatus.PAID_IN_FULL);
            account.setCurrentBalance(BigDecimal.ZERO);
            accountRepository.save(account);
        }
    }

    public List<Account> getDelinquentAccounts(int daysPastDue) {
        return accountRepository.findDelinquentAccounts(daysPastDue);
    }

    private Date getDatePlusDays(int days) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, days);
        return cal.getTime();
    }
}
