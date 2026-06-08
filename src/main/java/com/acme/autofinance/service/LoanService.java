package com.acme.autofinance.service;

import com.acme.autofinance.events.DomainEventPublisher;
import com.acme.autofinance.events.LateFeeRequiredEvent;
import com.acme.autofinance.model.Account;
import com.acme.autofinance.model.AccountStatus;
import com.acme.autofinance.model.CreditDecision;
import com.acme.autofinance.model.DealPackage;
import com.acme.autofinance.model.DealStatus;
import com.acme.autofinance.model.LoanApplication;
import com.acme.autofinance.model.LoanStatus;
import com.acme.autofinance.repository.AccountRepository;
import com.acme.autofinance.repository.DealPackageRepository;
import com.acme.autofinance.repository.DealerRepository;
import com.acme.autofinance.repository.LoanRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * God service: handles loan origination, credit decisions, account creation,
 * payment calculations, dealer settlement, AND portfolio reporting.
 *
 * This is intentionally a legacy anti-pattern — all business logic for multiple
 * bounded contexts lives in a single class with tight coupling to other services
 * and repositories across domain boundaries.
 */
@Service
public class LoanService {

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private DealerRepository dealerRepository;

    @Autowired
    private DealPackageRepository dealPackageRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AccountService accountService;

    @Autowired
    private DomainEventPublisher eventPublisher;

    // ========================================================================
    // LOAN ORIGINATION — should be its own bounded context
    // ========================================================================

    @Transactional
    public LoanApplication createApplication(LoanApplication application) {
        application.setApplicationNumber("LN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        application.setStatus(LoanStatus.SUBMITTED);
        application.setApplicationDate(new Date());

        LoanApplication saved = loanRepository.save(application);

        // Synchronous credit check — blocks the entire request
        CreditDecision decision = performCreditCheck(saved);

        if ("APPROVED".equals(decision.getDecision())) {
            saved.setStatus(LoanStatus.APPROVED);
            saved.setCreditScore(decision.getCreditScore());
            saved.setApprovedAmount(decision.getMaxApprovedAmount());
            saved.setInterestRate(decision.getOfferedRate());
            saved.setApprovalDate(new Date());

            // Calculate monthly payment inline
            saved.setMonthlyPayment(calculateMonthlyPayment(
                    decision.getMaxApprovedAmount(),
                    decision.getOfferedRate(),
                    saved.getTermMonths() != null ? saved.getTermMonths() : 60));

            // Synchronously create the account — tight coupling to account domain
            Account account = new Account();
            account.setAccountNumber("ACCT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            account.setLoanId(saved.getId());
            account.setCustomerName(saved.getApplicantName());
            account.setOriginalBalance(saved.getApprovedAmount());
            account.setCurrentBalance(saved.getApprovedAmount());
            account.setPayoffAmount(calculatePayoffAmount(saved));
            account.setDaysPastDue(0);
            account.setStatus(AccountStatus.CURRENT);

            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.MONTH, 1);
            account.setNextDueDate(cal.getTime());

            cal = Calendar.getInstance();
            cal.add(Calendar.MONTH, saved.getTermMonths() != null ? saved.getTermMonths() : 60);
            account.setMaturityDate(cal.getTime());

            accountRepository.save(account);

            // If dealer-submitted, update deal package status
            if (saved.getDealerId() != null) {
                updateDealPackageForApproval(saved);
            }
        } else {
            saved.setStatus(LoanStatus.DECLINED);
            saved.setCreditScore(decision.getCreditScore());
        }

        return loanRepository.save(saved);
    }

    public LoanApplication getApplication(Long id) {
        return loanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Loan application not found: " + id));
    }

    public LoanApplication getByApplicationNumber(String applicationNumber) {
        return loanRepository.findByApplicationNumber(applicationNumber)
                .orElseThrow(() -> new RuntimeException("Application not found: " + applicationNumber));
    }

    public List<LoanApplication> getByStatus(LoanStatus status) {
        return loanRepository.findByStatus(status);
    }

    @Transactional
    public LoanApplication updateTerms(Long id, BigDecimal amount, BigDecimal rate, Integer termMonths) {
        LoanApplication loan = getApplication(id);
        if (loan.getStatus() != LoanStatus.APPROVED && loan.getStatus() != LoanStatus.SUBMITTED) {
            throw new RuntimeException("Cannot update terms for loan in status: " + loan.getStatus());
        }

        if (amount != null) loan.setRequestedAmount(amount);
        if (rate != null) loan.setInterestRate(rate);
        if (termMonths != null) loan.setTermMonths(termMonths);

        // Recalculate monthly payment
        BigDecimal loanAmount = amount != null ? amount : loan.getApprovedAmount();
        BigDecimal loanRate = rate != null ? rate : loan.getInterestRate();
        int loanTerm = termMonths != null ? termMonths : (loan.getTermMonths() != null ? loan.getTermMonths() : 60);

        if (loanAmount != null && loanRate != null) {
            loan.setMonthlyPayment(calculateMonthlyPayment(loanAmount, loanRate, loanTerm));
        }

        // Synchronously update the account balance — cross-domain coupling
        Optional<Account> account = accountRepository.findByLoanId(id);
        if (account.isPresent() && amount != null) {
            Account acct = account.get();
            acct.setCurrentBalance(amount);
            acct.setPayoffAmount(calculatePayoffAmount(loan));
            accountRepository.save(acct);
        }

        return loanRepository.save(loan);
    }

    @Transactional
    public LoanApplication fundLoan(Long id) {
        LoanApplication loan = getApplication(id);
        if (loan.getStatus() != LoanStatus.APPROVED) {
            throw new RuntimeException("Can only fund approved loans, current status: " + loan.getStatus());
        }

        loan.setStatus(LoanStatus.FUNDED);
        loan.setFundingDate(new Date());

        // Update account status
        accountService.activateAccount(loan.getId());

        // Update deal package if dealer-originated
        if (loan.getDealerId() != null) {
            List<DealPackage> deals = dealPackageRepository.findByDealerId(loan.getDealerId());
            for (DealPackage deal : deals) {
                if (deal.getLoanId() != null && deal.getLoanId().equals(id)) {
                    deal.setStatus(DealStatus.FUNDED);
                    dealPackageRepository.save(deal);
                }
            }
        }

        loan.setStatus(LoanStatus.ACTIVE);
        return loanRepository.save(loan);
    }

    // ========================================================================
    // CREDIT DECISION — inline, should be its own service or external call
    // ========================================================================

    private CreditDecision performCreditCheck(LoanApplication application) {
        CreditDecision decision = new CreditDecision();
        decision.setLoanId(application.getId());
        decision.setDecisionDate(new Date());

        // Simulated credit score lookup — in production this would be an external
        // bureau call (Experian, TransUnion, Equifax) but we inline it here
        int creditScore = application.getCreditScore() != null ? application.getCreditScore() : 680;
        decision.setCreditScore(creditScore);

        // Hardcoded approval thresholds — business rules that should be externalized
        if (creditScore >= 720) {
            decision.setDecision("APPROVED");
            decision.setRiskTier("A");
            decision.setMaxApprovedAmount(application.getRequestedAmount());
            decision.setOfferedRate(new BigDecimal("3.99"));
            decision.setMaxTermMonths(84);
        } else if (creditScore >= 680) {
            decision.setDecision("APPROVED");
            decision.setRiskTier("B");
            decision.setMaxApprovedAmount(application.getRequestedAmount());
            decision.setOfferedRate(new BigDecimal("5.49"));
            decision.setMaxTermMonths(72);
        } else if (creditScore >= 620) {
            decision.setDecision("APPROVED");
            decision.setRiskTier("C");
            BigDecimal maxAmount = application.getRequestedAmount()
                    .multiply(new BigDecimal("0.85"))
                    .setScale(2, RoundingMode.HALF_UP);
            decision.setMaxApprovedAmount(maxAmount);
            decision.setOfferedRate(new BigDecimal("8.99"));
            decision.setMaxTermMonths(60);
        } else if (creditScore >= 560) {
            decision.setDecision("APPROVED");
            decision.setRiskTier("D");
            BigDecimal maxAmount = application.getRequestedAmount()
                    .multiply(new BigDecimal("0.70"))
                    .setScale(2, RoundingMode.HALF_UP);
            decision.setMaxApprovedAmount(maxAmount);
            decision.setOfferedRate(new BigDecimal("14.99"));
            decision.setMaxTermMonths(48);
        } else {
            decision.setDecision("DECLINED");
            decision.setRiskTier("E");
            decision.setDeclineReason("Credit score below minimum threshold of 560");
        }

        return decision;
    }

    // ========================================================================
    // PAYMENT CALCULATIONS — should be in PaymentService but lives here
    // ========================================================================

    public BigDecimal calculateMonthlyPayment(BigDecimal principal, BigDecimal annualRate, int termMonths) {
        if (principal == null || annualRate == null || termMonths <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal monthlyRate = annualRate.divide(new BigDecimal("1200"), 10, RoundingMode.HALF_UP);

        if (monthlyRate.compareTo(BigDecimal.ZERO) == 0) {
            return principal.divide(new BigDecimal(termMonths), 2, RoundingMode.HALF_UP);
        }

        // M = P * [r(1+r)^n] / [(1+r)^n - 1]
        BigDecimal onePlusR = BigDecimal.ONE.add(monthlyRate);
        BigDecimal power = onePlusR.pow(termMonths, new MathContext(15));
        BigDecimal numerator = principal.multiply(monthlyRate).multiply(power);
        BigDecimal denominator = power.subtract(BigDecimal.ONE);

        return numerator.divide(denominator, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculatePayoffAmount(LoanApplication loan) {
        if (loan.getApprovedAmount() == null || loan.getInterestRate() == null) {
            return loan.getRequestedAmount();
        }
        int term = loan.getTermMonths() != null ? loan.getTermMonths() : 60;
        BigDecimal monthly = calculateMonthlyPayment(loan.getApprovedAmount(), loan.getInterestRate(), term);
        return monthly.multiply(new BigDecimal(term)).setScale(2, RoundingMode.HALF_UP);
    }

    // ========================================================================
    // DEAL PACKAGE HANDLING — should be in DealerService
    // ========================================================================

    private void updateDealPackageForApproval(LoanApplication loan) {
        List<DealPackage> deals = dealPackageRepository.findByDealerId(loan.getDealerId());
        for (DealPackage deal : deals) {
            if (deal.getVehicleVin() != null && deal.getVehicleVin().equals(loan.getVehicleVin())) {
                deal.setLoanId(loan.getId());
                deal.setStatus(DealStatus.APPROVED);

                // Calculate dealer reserve inline — should be in DealerService
                BigDecimal reserveRate = new BigDecimal("1.50"); // hardcoded default
                deal.setDealerReserve(
                        loan.getApprovedAmount().multiply(reserveRate)
                                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP));

                // Calculate holdback inline
                BigDecimal holdbackPct = new BigDecimal("2.00"); // hardcoded default
                deal.setHoldbackAmount(
                        loan.getApprovedAmount().multiply(holdbackPct)
                                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP));

                dealPackageRepository.save(deal);
            }
        }
    }

    // ========================================================================
    // PORTFOLIO REPORTING — should be in ReportService
    // ========================================================================

    public Map<String, Object> getPortfolioSummary() {
        Map<String, Object> summary = new HashMap<>();

        // Direct SQL queries against shared tables — no separation of concerns
        Long totalLoans = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM loan_applications", Long.class);
        BigDecimal totalPortfolio = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(approved_amount), 0) FROM loan_applications WHERE status IN ('ACTIVE', 'FUNDED')",
                BigDecimal.class);
        Long delinquentCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM accounts WHERE days_past_due > 30", Long.class);
        BigDecimal totalPayments = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(payment_amount), 0) FROM payments WHERE status = 'COMPLETED'",
                BigDecimal.class);

        summary.put("totalLoans", totalLoans);
        summary.put("totalPortfolioValue", totalPortfolio);
        summary.put("delinquentAccounts", delinquentCount);
        summary.put("totalPaymentsReceived", totalPayments);

        // Hardcoded delinquency rate calculation
        if (totalLoans != null && totalLoans > 0 && delinquentCount != null) {
            double delinquencyRate = (double) delinquentCount / totalLoans * 100;
            summary.put("delinquencyRate", String.format("%.2f%%", delinquencyRate));
        } else {
            summary.put("delinquencyRate", "0.00%");
        }

        // Status breakdown — reaches into every domain's table
        summary.put("loansByStatus", getLoanStatusBreakdown());
        summary.put("paymentsByStatus", getPaymentStatusBreakdown());
        summary.put("accountsByStatus", getAccountStatusBreakdown());

        return summary;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Long> getLoanStatusBreakdown() {
        Map<String, Long> breakdown = new HashMap<>();
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT status, COUNT(*) as cnt FROM loan_applications GROUP BY status");
        for (Map<String, Object> row : rows) {
            breakdown.put((String) row.get("STATUS"), (Long) row.get("CNT"));
        }
        return breakdown;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Long> getPaymentStatusBreakdown() {
        Map<String, Long> breakdown = new HashMap<>();
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT status, COUNT(*) as cnt FROM payments GROUP BY status");
        for (Map<String, Object> row : rows) {
            breakdown.put((String) row.get("STATUS"), (Long) row.get("CNT"));
        }
        return breakdown;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Long> getAccountStatusBreakdown() {
        Map<String, Long> breakdown = new HashMap<>();
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT status, COUNT(*) as cnt FROM accounts GROUP BY status");
        for (Map<String, Object> row : rows) {
            breakdown.put((String) row.get("STATUS"), (Long) row.get("CNT"));
        }
        return breakdown;
    }

    public Map<String, Object> getDelinquencyReport() {
        Map<String, Object> report = new HashMap<>();

        List<Account> delinquent30 = accountRepository.findDelinquentAccounts(30);
        List<Account> delinquent60 = accountRepository.findDelinquentAccounts(60);
        List<Account> delinquent90 = accountRepository.findDelinquentAccounts(90);

        report.put("delinquent30Plus", delinquent30.size());
        report.put("delinquent60Plus", delinquent60.size());
        report.put("delinquent90Plus", delinquent90.size());

        BigDecimal totalDelinquentBalance = BigDecimal.ZERO;
        for (Account acct : delinquent30) {
            if (acct.getCurrentBalance() != null) {
                totalDelinquentBalance = totalDelinquentBalance.add(acct.getCurrentBalance());
            }
        }
        report.put("totalDelinquentBalance", totalDelinquentBalance);

        // TILA/ECOA compliance stub — regulatory reporting logic mixed in
        report.put("tilaCompliant", true);
        report.put("ecoaCompliant", true);
        report.put("reportDate", new Date());

        return report;
    }

    // ========================================================================
    // BATCH OPERATIONS — mixed concerns across all domains
    // ========================================================================

    @Transactional
    public Map<String, Object> runEndOfDayProcessing() {
        Map<String, Object> results = new HashMap<>();
        int accountsUpdated = 0;
        int lateFeesAssessed = 0;

        // Update days past due for all accounts — should be AccountService's job
        List<Account> allAccounts = accountRepository.findByStatus(AccountStatus.CURRENT);
        for (Account account : allAccounts) {
            if (account.getNextDueDate() != null && account.getNextDueDate().before(new Date())) {
                int daysPastDue = calculateDaysPastDue(account.getNextDueDate());
                account.setDaysPastDue(daysPastDue);

                if (daysPastDue > 30) {
                    account.setStatus(AccountStatus.DELINQUENT_30);
                }
                if (daysPastDue > 60) {
                    account.setStatus(AccountStatus.DELINQUENT_60);
                }
                if (daysPastDue > 90) {
                    account.setStatus(AccountStatus.DELINQUENT_90);
                }

                accountRepository.save(account);
                accountsUpdated++;

                // Assess late fees — request the Payment context via an event
                // instead of calling PaymentService synchronously.
                if (daysPastDue > 15) {
                    eventPublisher.publish(new LateFeeRequiredEvent(account.getLoanId(), daysPastDue));
                    lateFeesAssessed++;
                }

                // Update corresponding loan status — cross-domain synchronous update
                Optional<LoanApplication> loan = loanRepository.findById(account.getLoanId());
                if (loan.isPresent() && daysPastDue > 30) {
                    loan.get().setStatus(LoanStatus.DELINQUENT);
                    loanRepository.save(loan.get());
                }
            }
        }

        results.put("accountsUpdated", accountsUpdated);
        results.put("lateFeesAssessed", lateFeesAssessed);
        results.put("processedAt", new Date());
        return results;
    }

    private int calculateDaysPastDue(Date dueDate) {
        long diffMillis = new Date().getTime() - dueDate.getTime();
        return (int) (diffMillis / (1000 * 60 * 60 * 24));
    }

    public List<LoanApplication> getAllApplications() {
        return loanRepository.findAll();
    }
}
