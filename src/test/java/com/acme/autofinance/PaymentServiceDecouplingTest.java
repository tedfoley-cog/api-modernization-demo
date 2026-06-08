package com.acme.autofinance;

import com.acme.autofinance.events.PaymentCompletedEvent;
import com.acme.autofinance.events.PaymentSubmittedEvent;
import com.acme.autofinance.model.Account;
import com.acme.autofinance.model.AccountStatus;
import com.acme.autofinance.model.LoanApplication;
import com.acme.autofinance.model.LoanStatus;
import com.acme.autofinance.model.Payment;
import com.acme.autofinance.model.PaymentMethod;
import com.acme.autofinance.repository.AccountRepository;
import com.acme.autofinance.repository.LoanRepository;
import com.acme.autofinance.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the structural decoupling of {@link PaymentService} from the Loan and
 * Account contexts, plus the end-to-end event flow that replaces the old
 * synchronous cross-domain calls.
 */
@SpringBootTest
@RecordApplicationEvents
@Transactional
class PaymentServiceDecouplingTest {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private ApplicationEvents events;

    @Test
    void paymentServiceHasNoCrossDomainRepositoryFields() {
        for (Field field : PaymentService.class.getDeclaredFields()) {
            Class<?> type = field.getType();
            assertFalse(LoanRepository.class.isAssignableFrom(type),
                    "PaymentService must not inject LoanRepository directly (use LoanValidationPort)");
            assertFalse(AccountRepository.class.isAssignableFrom(type),
                    "PaymentService must not inject AccountRepository directly (use LoanValidationPort)");
        }
    }

    @Test
    void paymentServiceDoesNotImportLoanOrAccountRepositories() {
        // The decoupling should be visible at field level for every injected dependency.
        boolean hasLoanRepo = false;
        boolean hasAccountRepo = false;
        for (Field field : PaymentService.class.getDeclaredFields()) {
            if (field.getType().equals(LoanRepository.class)) {
                hasLoanRepo = true;
            }
            if (field.getType().equals(AccountRepository.class)) {
                hasAccountRepo = true;
            }
        }
        assertFalse(hasLoanRepo);
        assertFalse(hasAccountRepo);
    }

    @Test
    void eventFlowEndToEndUpdatesStateViaListeners() {
        LoanApplication loan = new LoanApplication();
        loan.setApplicationNumber("LN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        loan.setApplicantName("Decoupling Test");
        loan.setApprovedAmount(new BigDecimal("8000.00"));
        loan.setInterestRate(new BigDecimal("6.00"));
        loan.setTermMonths(48);
        loan.setStatus(LoanStatus.ACTIVE);
        loan = loanRepository.save(loan);

        Account account = new Account();
        account.setAccountNumber("ACCT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        account.setLoanId(loan.getId());
        account.setCustomerName("Decoupling Test");
        account.setOriginalBalance(new BigDecimal("8000.00"));
        account.setCurrentBalance(new BigDecimal("8000.00"));
        account.setDaysPastDue(0);
        account.setStatus(AccountStatus.CURRENT);
        account = accountRepository.save(account);

        Payment payment = new Payment();
        payment.setLoanId(loan.getId());
        payment.setPaymentAmount(new BigDecimal("600.00"));
        payment.setPaymentMethod(PaymentMethod.ACH);
        payment.setAchRoutingNumber("123456789");
        payment.setAchAccountNumber("000999888");

        paymentService.submitPayment(payment);

        // submit -> event published -> listeners fire -> account balance updated
        assertEquals(1, events.stream(PaymentSubmittedEvent.class)
                .filter(e -> e.getPaymentId() != null).count());
        assertEquals(1, events.stream(PaymentCompletedEvent.class).count());

        Account updated = accountRepository.findById(account.getId()).orElseThrow(AssertionError::new);
        assertTrue(updated.getCurrentBalance().compareTo(new BigDecimal("8000.00")) < 0,
                "account balance should have been updated by the listener, not by PaymentService");
    }
}
