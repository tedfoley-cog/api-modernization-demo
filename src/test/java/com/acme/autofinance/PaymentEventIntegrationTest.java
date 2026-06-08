package com.acme.autofinance;

import com.acme.autofinance.events.DomainEventPublisher;
import com.acme.autofinance.events.LateFeeAssessedEvent;
import com.acme.autofinance.events.LateFeeRequiredEvent;
import com.acme.autofinance.events.LoanPayoffEvent;
import com.acme.autofinance.events.PaymentCompletedEvent;
import com.acme.autofinance.events.PaymentFailedEvent;
import com.acme.autofinance.events.PaymentSubmittedEvent;
import com.acme.autofinance.model.Account;
import com.acme.autofinance.model.AccountStatus;
import com.acme.autofinance.model.LoanApplication;
import com.acme.autofinance.model.LoanStatus;
import com.acme.autofinance.model.Payment;
import com.acme.autofinance.model.PaymentMethod;
import com.acme.autofinance.model.PaymentStatus;
import com.acme.autofinance.repository.AccountRepository;
import com.acme.autofinance.repository.LoanRepository;
import com.acme.autofinance.repository.PaymentRepository;
import com.acme.autofinance.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end validation of the event-driven payment flow: publishing,
 * listener reactions, and the no-side-effect-on-failure guarantee.
 */
@SpringBootTest
@RecordApplicationEvents
@Transactional
class PaymentEventIntegrationTest {

    private static final String VALID_ROUTING = "123456789";
    private static final String INVALID_ROUTING = "123";

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private DomainEventPublisher eventPublisher;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private ApplicationEvents events;

    private LoanApplication newLoan(BigDecimal approvedAmount) {
        LoanApplication loan = new LoanApplication();
        loan.setApplicationNumber("LN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        loan.setApplicantName("Test Applicant");
        loan.setApprovedAmount(approvedAmount);
        loan.setInterestRate(new BigDecimal("5.00"));
        loan.setTermMonths(60);
        loan.setStatus(LoanStatus.ACTIVE);
        return loanRepository.save(loan);
    }

    private Account newAccount(Long loanId, BigDecimal balance) {
        Account account = new Account();
        account.setAccountNumber("ACCT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        account.setLoanId(loanId);
        account.setCustomerName("Test Applicant");
        account.setOriginalBalance(balance);
        account.setCurrentBalance(balance);
        account.setDaysPastDue(10);
        account.setStatus(AccountStatus.CURRENT);
        return accountRepository.save(account);
    }

    private Payment achPayment(Long loanId, BigDecimal amount, String routing) {
        Payment payment = new Payment();
        payment.setLoanId(loanId);
        payment.setPaymentAmount(amount);
        payment.setPaymentMethod(PaymentMethod.ACH);
        payment.setAchRoutingNumber(routing);
        payment.setAchAccountNumber("000123456");
        return payment;
    }

    @Test
    void submittingPaymentPublishesPaymentSubmittedEvent() {
        LoanApplication loan = newLoan(new BigDecimal("10000.00"));
        newAccount(loan.getId(), new BigDecimal("10000.00"));

        paymentService.submitPayment(achPayment(loan.getId(), new BigDecimal("500.00"), VALID_ROUTING));

        long submitted = events.stream(PaymentSubmittedEvent.class)
                .filter(e -> e.getLoanId().equals(loan.getId()))
                .count();
        assertEquals(1, submitted, "expected exactly one PaymentSubmittedEvent");
    }

    @Test
    void completingAchPaymentPublishesPaymentCompletedEvent() {
        LoanApplication loan = newLoan(new BigDecimal("10000.00"));
        newAccount(loan.getId(), new BigDecimal("10000.00"));

        Payment saved = paymentService.submitPayment(
                achPayment(loan.getId(), new BigDecimal("500.00"), VALID_ROUTING));

        assertEquals(PaymentStatus.COMPLETED, saved.getStatus());
        long completed = events.stream(PaymentCompletedEvent.class)
                .filter(e -> e.getLoanId().equals(loan.getId()))
                .count();
        assertEquals(1, completed, "expected exactly one PaymentCompletedEvent");
    }

    @Test
    void accountBalanceListenerUpdatesBalanceOnCompletion() {
        LoanApplication loan = newLoan(new BigDecimal("10000.00"));
        Account account = newAccount(loan.getId(), new BigDecimal("10000.00"));

        paymentService.submitPayment(achPayment(loan.getId(), new BigDecimal("500.00"), VALID_ROUTING));

        Account updated = accountRepository.findById(account.getId()).orElseThrow(AssertionError::new);
        assertTrue(updated.getCurrentBalance().compareTo(new BigDecimal("10000.00")) < 0,
                "balance should be reduced by the principal portion");
        assertEquals(0, updated.getDaysPastDue(), "days past due should be reset on payment");
    }

    @Test
    void loanStatusListenerMarksLoanPaidOffWhenPaymentsExceedApproved() {
        LoanApplication loan = newLoan(new BigDecimal("400.00"));
        Account account = newAccount(loan.getId(), new BigDecimal("400.00"));

        // Single payment that meets the approved amount triggers payoff.
        paymentService.submitPayment(achPayment(loan.getId(), new BigDecimal("400.00"), VALID_ROUTING));

        LoanApplication updatedLoan = loanRepository.findById(loan.getId()).orElseThrow(AssertionError::new);
        Account updatedAccount = accountRepository.findById(account.getId()).orElseThrow(AssertionError::new);

        assertEquals(LoanStatus.PAID_OFF, updatedLoan.getStatus(), "loan should be paid off");
        assertEquals(AccountStatus.PAID_IN_FULL, updatedAccount.getStatus(), "account should be closed");
        assertEquals(1, events.stream(LoanPayoffEvent.class)
                .filter(e -> e.getLoanId().equals(loan.getId())).count());
    }

    @Test
    void failedPaymentPublishesFailedEventAndDoesNotUpdateAccountOrLoan() {
        LoanApplication loan = newLoan(new BigDecimal("10000.00"));
        Account account = newAccount(loan.getId(), new BigDecimal("10000.00"));

        Payment saved = paymentService.submitPayment(
                achPayment(loan.getId(), new BigDecimal("500.00"), INVALID_ROUTING));

        assertEquals(PaymentStatus.FAILED, saved.getStatus());
        assertEquals(1, events.stream(PaymentFailedEvent.class)
                .filter(e -> e.getLoanId().equals(loan.getId())).count());
        assertEquals(0, events.stream(PaymentCompletedEvent.class)
                .filter(e -> e.getLoanId().equals(loan.getId())).count());

        Account untouched = accountRepository.findById(account.getId()).orElseThrow(AssertionError::new);
        assertEquals(0, untouched.getCurrentBalance().compareTo(new BigDecimal("10000.00")),
                "balance must be unchanged on failed payment");
        LoanApplication untouchedLoan = loanRepository.findById(loan.getId()).orElseThrow(AssertionError::new);
        assertEquals(LoanStatus.ACTIVE, untouchedLoan.getStatus(),
                "loan status must be unchanged on failed payment");
    }

    @Test
    void lateFeeRequiredEventCreatesFeeRecordAndPublishesAssessedEvent() {
        LoanApplication loan = newLoan(new BigDecimal("10000.00"));
        newAccount(loan.getId(), new BigDecimal("10000.00"));

        eventPublisher.publish(new LateFeeRequiredEvent(loan.getId(), 20));

        List<Payment> fees = paymentRepository.findByLoanIdAndStatus(loan.getId(), PaymentStatus.PENDING);
        assertEquals(1, fees.size(), "a single fee record should be created");
        assertEquals(0, fees.get(0).getLateFee().compareTo(new BigDecimal("25.00")),
                "flat late fee expected for <=30 days past due");
        assertEquals(1, events.stream(LateFeeAssessedEvent.class)
                .filter(e -> e.getLoanId().equals(loan.getId())).count());
    }

    @Test
    void submittingToUnknownLoanFailsValidationViaPort() {
        // No loan/account created — the anti-corruption port reports it missing.
        Payment payment = achPayment(999999L, new BigDecimal("100.00"), VALID_ROUTING);
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> paymentService.submitPayment(payment));
        assertTrue(ex.getMessage().contains("Loan not found"));
    }
}
