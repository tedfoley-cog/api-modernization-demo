package com.acme.autofinance.account.service;

import com.acme.autofinance.account.domain.Account;
import com.acme.autofinance.account.domain.AccountStatus;
import com.acme.autofinance.account.repository.AccountRepository;
import com.acme.autofinance.events.AccountBalanceUpdated;
import com.acme.autofinance.events.AccountCreated;
import com.acme.autofinance.events.AccountDelinquent;
import com.acme.autofinance.messaging.DomainEventPublisher;
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
import java.util.UUID;

/**
 * Account servicing for the account-servicing bounded context. Owns the account
 * aggregate and all balance/payoff/delinquency logic. Loan facts (interest rate,
 * term) are projected onto the account from loan-origination events; payment
 * facts arrive from payment-processing events. The service never reads another
 * domain's database or beans, and it broadcasts account occurrences as domain
 * events via {@link DomainEventPublisher} for other contexts to react to.
 */
@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final DomainEventPublisher eventPublisher;

    public AccountService(AccountRepository accountRepository, DomainEventPublisher eventPublisher) {
        this.accountRepository = accountRepository;
        this.eventPublisher = eventPublisher;
    }

    // ========================================================================
    // Query/command API (backs the REST controller)
    // ========================================================================

    public Account getAccountDetails(Long accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountId));
    }

    public Account getByAccountNumber(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNumber));
    }

    @Transactional
    public Account updateAddress(Long accountId, String newAddress) {
        Account account = getAccountDetails(accountId);
        account.setMailingAddress(newAddress);
        return accountRepository.save(account);
    }

    /**
     * Payoff quote computed entirely from account-owned state: current balance,
     * the projected interest rate, and account-owned outstanding fees. No loan or
     * payment lookups.
     */
    public Map<String, Object> calculatePayoffQuote(Long accountId) {
        Account account = getAccountDetails(accountId);
        Map<String, Object> quote = new HashMap<>();

        BigDecimal balance = account.getCurrentBalance() != null
                ? account.getCurrentBalance() : BigDecimal.ZERO;

        BigDecimal dailyRate = BigDecimal.ZERO;
        if (account.getInterestRate() != null) {
            dailyRate = account.getInterestRate()
                    .divide(new BigDecimal("36500"), 10, RoundingMode.HALF_UP);
        }

        BigDecimal perDiem = balance.multiply(dailyRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal tenDayInterest = perDiem.multiply(new BigDecimal("10")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal outstandingFees = account.getOutstandingFees() != null
                ? account.getOutstandingFees() : BigDecimal.ZERO;

        BigDecimal totalPayoff = balance.add(tenDayInterest).add(outstandingFees);

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

    /**
     * Closes out an account early. Zeroes the balance and marks the account
     * terminated using only account-owned state, then publishes the balance
     * change. The associated loan is <em>not</em> mutated here — loan-origination
     * reacts to the balance/close event on its own.
     */
    @Transactional
    public Map<String, Object> processEarlyTermination(Long accountId) {
        Account account = getAccountDetails(accountId);
        Map<String, Object> result = new HashMap<>();

        Map<String, Object> payoff = calculatePayoffQuote(accountId);

        BigDecimal previousBalance = account.getCurrentBalance() != null
                ? account.getCurrentBalance() : BigDecimal.ZERO;

        account.setStatus(AccountStatus.EARLY_TERMINATION);
        account.setCurrentBalance(BigDecimal.ZERO);
        accountRepository.save(account);

        if (previousBalance.compareTo(BigDecimal.ZERO) != 0) {
            eventPublisher.publish(new AccountBalanceUpdated(
                    account.getId(),
                    previousBalance,
                    BigDecimal.ZERO,
                    previousBalance,
                    "EARLY_TERMINATION"));
        }

        result.put("accountNumber", account.getAccountNumber());
        result.put("terminationDate", new Date());
        result.put("finalPayoffAmount", payoff.get("totalPayoffAmount"));
        result.put("status", "TERMINATED");

        return result;
    }

    public List<Account> getDelinquentAccounts(int daysPastDue) {
        return accountRepository.findDelinquentAccounts(daysPastDue);
    }

    // ========================================================================
    // Event-driven state changes (invoked by the Kafka listener)
    // ========================================================================

    /**
     * Creates and activates an account in response to a funded loan. Idempotent:
     * if an account already exists for the loan, no second account is created and
     * no duplicate {@link AccountCreated} is published. Applicant contact details
     * are not present in the authoritative {@code LoanFunded} payload, so they are
     * left as optional projected data to be enriched later.
     *
     * @return the created account, or {@code null} if one already existed.
     */
    @Transactional
    public Account createAccountForLoan(Long loanId, String customerName, BigDecimal originalBalance,
                                        BigDecimal interestRate, Integer termMonths) {
        if (loanId != null && accountRepository.findByLoanId(loanId).isPresent()) {
            return null;
        }

        Account account = new Account();
        account.setAccountNumber("ACCT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        account.setLoanId(loanId);
        account.setCustomerName(customerName);
        account.setOriginalBalance(originalBalance);
        account.setCurrentBalance(originalBalance);
        account.setInterestRate(interestRate);
        account.setTermMonths(termMonths);
        account.setOutstandingFees(BigDecimal.ZERO);
        account.setDaysPastDue(0);
        account.setStatus(AccountStatus.CURRENT);

        int term = termMonths != null ? termMonths : 60;
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MONTH, 1);
        account.setNextDueDate(cal.getTime());
        cal = Calendar.getInstance();
        cal.add(Calendar.MONTH, term);
        account.setMaturityDate(cal.getTime());

        Account saved = accountRepository.save(account);

        eventPublisher.publish(new AccountCreated(
                saved.getId(),
                saved.getAccountNumber(),
                saved.getLoanId(),
                saved.getCustomerName(),
                saved.getOriginalBalance()));

        return saved;
    }

    /** Records that a payment was received (in-flight); does not move the balance. */
    @Transactional
    public void recordPaymentReceived(Long loanId, Date receivedAt) {
        Optional<Account> accountOpt = accountRepository.findByLoanId(loanId);
        if (!accountOpt.isPresent()) {
            return;
        }
        Account account = accountOpt.get();
        account.setLastPaymentDate(receivedAt != null ? receivedAt : new Date());
        accountRepository.save(account);
    }

    /**
     * Applies a completed payment to the authoritative balance and publishes
     * {@link AccountBalanceUpdated}. The account context owns the balance; the
     * {@code principalApplied} from the event drives the decrement.
     */
    @Transactional
    public void applyProcessedPayment(Long loanId, BigDecimal principalApplied) {
        Optional<Account> accountOpt = accountRepository.findByLoanId(loanId);
        if (!accountOpt.isPresent() || principalApplied == null) {
            return;
        }
        Account account = accountOpt.get();
        BigDecimal previousBalance = account.getCurrentBalance() != null
                ? account.getCurrentBalance() : BigDecimal.ZERO;

        BigDecimal newBalance = previousBalance.subtract(principalApplied);
        if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
            newBalance = BigDecimal.ZERO;
        }

        account.setCurrentBalance(newBalance);
        account.setLastPaymentDate(new Date());
        account.setDaysPastDue(0);
        if (account.getStatus() == AccountStatus.DELINQUENT_30
                || account.getStatus() == AccountStatus.DELINQUENT_60
                || account.getStatus() == AccountStatus.DELINQUENT_90) {
            account.setStatus(AccountStatus.CURRENT);
        }
        accountRepository.save(account);

        eventPublisher.publish(new AccountBalanceUpdated(
                account.getId(),
                previousBalance,
                newBalance,
                principalApplied,
                "PAYMENT"));
    }

    /** Reduces account-owned outstanding fees by the fee portion of a payment. */
    @Transactional
    public void applyFeeAllocation(Long loanId, BigDecimal feeAmount) {
        if (feeAmount == null || feeAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        Optional<Account> accountOpt = accountRepository.findByLoanId(loanId);
        if (!accountOpt.isPresent()) {
            return;
        }
        Account account = accountOpt.get();
        BigDecimal current = account.getOutstandingFees() != null
                ? account.getOutstandingFees() : BigDecimal.ZERO;
        BigDecimal remaining = current.subtract(feeAmount);
        if (remaining.compareTo(BigDecimal.ZERO) < 0) {
            remaining = BigDecimal.ZERO;
        }
        account.setOutstandingFees(remaining);
        accountRepository.save(account);
    }

    /**
     * Assesses delinquency for an account and publishes {@link AccountDelinquent}.
     * Buckets and status are derived from days past due using account-owned state.
     */
    @Transactional
    public void assessDelinquency(Long accountId, int daysPastDue) {
        Optional<Account> accountOpt = accountRepository.findById(accountId);
        if (!accountOpt.isPresent()) {
            return;
        }
        Account account = accountOpt.get();
        account.setDaysPastDue(daysPastDue);
        account.setStatus(statusForDaysPastDue(daysPastDue, account.getStatus()));
        accountRepository.save(account);

        eventPublisher.publish(new AccountDelinquent(
                account.getId(),
                account.getLoanId(),
                daysPastDue,
                delinquencyBucket(daysPastDue),
                account.getCurrentBalance()));
    }

    /**
     * End-of-day scan: advances days-past-due for current accounts whose next due
     * date has passed and assesses delinquency. Replaces the monolith's cross-domain
     * batch, operating only on account-owned state.
     *
     * @return number of accounts marked delinquent.
     */
    @Transactional
    public int runEndOfDayDelinquencyProcessing() {
        int delinquent = 0;
        Date now = new Date();
        for (Account account : accountRepository.findByStatus(AccountStatus.CURRENT)) {
            if (account.getNextDueDate() != null && account.getNextDueDate().before(now)) {
                int daysPastDue = calculateDaysPastDue(account.getNextDueDate());
                if (daysPastDue > 30) {
                    assessDelinquency(account.getId(), daysPastDue);
                    delinquent++;
                }
            }
        }
        return delinquent;
    }

    private static AccountStatus statusForDaysPastDue(int daysPastDue, AccountStatus current) {
        if (daysPastDue > 90) {
            return AccountStatus.DELINQUENT_90;
        }
        if (daysPastDue > 60) {
            return AccountStatus.DELINQUENT_60;
        }
        if (daysPastDue > 30) {
            return AccountStatus.DELINQUENT_30;
        }
        return current != null ? current : AccountStatus.CURRENT;
    }

    private static String delinquencyBucket(int daysPastDue) {
        if (daysPastDue >= 90) {
            return "90+";
        }
        if (daysPastDue >= 60) {
            return "60-89";
        }
        if (daysPastDue >= 30) {
            return "30-59";
        }
        return "CURRENT";
    }

    private int calculateDaysPastDue(Date dueDate) {
        long diffMillis = new Date().getTime() - dueDate.getTime();
        return (int) (diffMillis / (1000 * 60 * 60 * 24));
    }

    private Date getDatePlusDays(int days) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, days);
        return cal.getTime();
    }
}
