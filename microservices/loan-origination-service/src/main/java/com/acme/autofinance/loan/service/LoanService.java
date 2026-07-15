package com.acme.autofinance.loan.service;

import com.acme.autofinance.events.CreditDecisionMade;
import com.acme.autofinance.events.LoanApplicationSubmitted;
import com.acme.autofinance.events.LoanFunded;
import com.acme.autofinance.loan.domain.CreditDecision;
import com.acme.autofinance.loan.domain.LoanApplication;
import com.acme.autofinance.loan.domain.LoanStatus;
import com.acme.autofinance.loan.repository.LoanRepository;
import com.acme.autofinance.messaging.DomainEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Loan origination for the loan-origination bounded context. Owns exactly four
 * responsibilities: loan application intake, credit decisioning, terms, and
 * funding. Everything the legacy God service also did — account creation, payment
 * and late-fee orchestration, dealer/deal-package mutation, portfolio and
 * delinquency reporting, and end-of-day batch processing — has been removed.
 *
 * <p>The service depends only on its own {@link LoanRepository} and a
 * {@link DomainEventPublisher}; it never reads or writes another domain's database
 * or beans. Other contexts react to the events it publishes:
 * {@link LoanApplicationSubmitted}, {@link CreditDecisionMade}, and
 * {@link LoanFunded}.
 */
@Service
public class LoanService {

    private final LoanRepository loanRepository;
    private final DomainEventPublisher eventPublisher;

    public LoanService(LoanRepository loanRepository, DomainEventPublisher eventPublisher) {
        this.loanRepository = loanRepository;
        this.eventPublisher = eventPublisher;
    }

    // ========================================================================
    // Loan application + credit decisioning
    // ========================================================================

    @Transactional
    public LoanApplication createApplication(LoanApplication application) {
        application.setApplicationNumber("LN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        application.setStatus(LoanStatus.SUBMITTED);
        application.setApplicationDate(new Date());

        LoanApplication saved = loanRepository.save(application);

        eventPublisher.publish(new LoanApplicationSubmitted(
                saved.getId(),
                saved.getApplicationNumber(),
                saved.getApplicantName(),
                saved.getRequestedAmount(),
                saved.getVehicleVin(),
                saved.getDealerId()));

        CreditDecision decision = performCreditCheck(saved);

        if ("APPROVED".equals(decision.getDecision())) {
            saved.setStatus(LoanStatus.APPROVED);
            saved.setCreditScore(decision.getCreditScore());
            saved.setApprovedAmount(decision.getMaxApprovedAmount());
            saved.setInterestRate(decision.getOfferedRate());
            saved.setApprovalDate(new Date());
            saved.setMonthlyPayment(calculateMonthlyPayment(
                    decision.getMaxApprovedAmount(),
                    decision.getOfferedRate(),
                    saved.getTermMonths() != null ? saved.getTermMonths() : 60));
        } else {
            saved.setStatus(LoanStatus.DECLINED);
            saved.setCreditScore(decision.getCreditScore());
        }

        LoanApplication decided = loanRepository.save(saved);

        eventPublisher.publish(new CreditDecisionMade(
                decided.getId(),
                decision.getDecision(),
                decision.getCreditScore(),
                decision.getRiskTier(),
                decision.getMaxApprovedAmount(),
                decision.getOfferedRate()));

        return decided;
    }

    public LoanApplication getApplication(Long id) {
        return loanRepository.findById(id)
                .orElseThrow(() -> new LoanNotFoundException("Loan application not found: " + id));
    }

    public LoanApplication getByApplicationNumber(String applicationNumber) {
        return loanRepository.findByApplicationNumber(applicationNumber)
                .orElseThrow(() -> new LoanNotFoundException("Application not found: " + applicationNumber));
    }

    public List<LoanApplication> getByStatus(LoanStatus status) {
        return loanRepository.findByStatus(status);
    }

    public List<LoanApplication> getAllApplications() {
        return loanRepository.findAll();
    }

    // ========================================================================
    // Terms
    // ========================================================================

    @Transactional
    public LoanApplication updateTerms(Long id, BigDecimal amount, BigDecimal rate, Integer termMonths) {
        LoanApplication loan = getApplication(id);
        if (loan.getStatus() != LoanStatus.APPROVED && loan.getStatus() != LoanStatus.SUBMITTED) {
            throw new IllegalStateException("Cannot update terms for loan in status: " + loan.getStatus());
        }

        if (amount != null) loan.setRequestedAmount(amount);
        if (rate != null) loan.setInterestRate(rate);
        if (termMonths != null) loan.setTermMonths(termMonths);

        BigDecimal loanAmount = amount != null ? amount : loan.getApprovedAmount();
        BigDecimal loanRate = rate != null ? rate : loan.getInterestRate();
        int loanTerm = termMonths != null ? termMonths : (loan.getTermMonths() != null ? loan.getTermMonths() : 60);

        if (loanAmount != null && loanRate != null) {
            loan.setMonthlyPayment(calculateMonthlyPayment(loanAmount, loanRate, loanTerm));
        }

        return loanRepository.save(loan);
    }

    // ========================================================================
    // Funding
    // ========================================================================

    @Transactional
    public LoanApplication fundLoan(Long id) {
        LoanApplication loan = getApplication(id);
        if (loan.getStatus() != LoanStatus.APPROVED) {
            throw new IllegalStateException("Can only fund approved loans, current status: " + loan.getStatus());
        }

        loan.setStatus(LoanStatus.FUNDED);
        loan.setFundingDate(new Date());
        loan.setStatus(LoanStatus.ACTIVE);
        LoanApplication funded = loanRepository.save(loan);

        eventPublisher.publish(new LoanFunded(
                funded.getId(),
                funded.getApplicationNumber(),
                toLocalDate(funded.getFundingDate()),
                funded.getApprovedAmount(),
                funded.getInterestRate(),
                funded.getTermMonths()));

        return funded;
    }

    // ========================================================================
    // Asynchronous command: dealer submission creates a loan application
    // ========================================================================

    /**
     * Originates a loan application from a dealer's deal-package submission. The
     * financed amount is derived from the sale price net of down payment and
     * trade-in. The applicant name is not present in the dealer submission, so a
     * placeholder is recorded to be enriched later. This never mutates dealer or
     * deal-package state — the dealer context reacts to the loan events instead.
     */
    @Transactional
    public LoanApplication createApplicationFromDealSubmission(String dealNumber, Long dealerId, String vehicleVin,
                                                               BigDecimal salePrice, BigDecimal downPayment,
                                                               BigDecimal tradeInValue) {
        LoanApplication application = new LoanApplication();
        application.setDealerId(dealerId);
        application.setVehicleVin(vehicleVin);
        application.setApplicantName("DEALER SUBMISSION " + dealNumber);
        application.setRequestedAmount(financedAmount(salePrice, downPayment, tradeInValue));
        return createApplication(application);
    }

    private static BigDecimal financedAmount(BigDecimal salePrice, BigDecimal downPayment, BigDecimal tradeInValue) {
        BigDecimal amount = salePrice != null ? salePrice : BigDecimal.ZERO;
        if (downPayment != null) {
            amount = amount.subtract(downPayment);
        }
        if (tradeInValue != null) {
            amount = amount.subtract(tradeInValue);
        }
        return amount.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : amount;
    }

    // ========================================================================
    // Credit decisioning + payment math (loan-owned)
    // ========================================================================

    private CreditDecision performCreditCheck(LoanApplication application) {
        CreditDecision decision = new CreditDecision();
        decision.setLoanId(application.getId());
        decision.setDecisionDate(new Date());

        int creditScore = application.getCreditScore() != null ? application.getCreditScore() : 680;
        decision.setCreditScore(creditScore);

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

    // ========================================================================
    // Local loan-lifecycle projection (driven by payment events)
    // ========================================================================

    /**
     * Advances the loan's own lifecycle when a payment completes. Reads/writes only
     * loan-owned state: an active loan whose balance reaches zero is marked
     * {@link LoanStatus#PAID_OFF}. Account/payment state is never mutated here.
     *
     * @return {@code true} if a loan row was found and projected.
     */
    @Transactional
    public boolean projectPaymentProcessed(Long loanId, BigDecimal newBalance) {
        if (loanId == null) {
            return false;
        }
        return loanRepository.findById(loanId).map(loan -> {
            if (newBalance != null && newBalance.compareTo(BigDecimal.ZERO) <= 0
                    && loan.getStatus() != LoanStatus.PAID_OFF) {
                loan.setStatus(LoanStatus.PAID_OFF);
                loanRepository.save(loan);
            }
            return true;
        }).orElse(false);
    }

    private static java.time.LocalDate toLocalDate(Date date) {
        if (date == null) {
            return java.time.LocalDate.now();
        }
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }
}
