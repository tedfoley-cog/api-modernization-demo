package com.acme.autofinance.account.service;

import com.acme.autofinance.account.domain.Account;
import com.acme.autofinance.account.domain.AccountStatus;
import com.acme.autofinance.account.repository.AccountRepository;
import com.acme.autofinance.events.AccountBalanceUpdated;
import com.acme.autofinance.events.AccountCreated;
import com.acme.autofinance.events.AccountDelinquent;
import com.acme.autofinance.messaging.DomainEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock private AccountRepository accountRepository;
    @Mock private DomainEventPublisher eventPublisher;

    private AccountService service;

    @BeforeEach
    void setUp() {
        service = new AccountService(accountRepository, eventPublisher);
    }

    private void assignIdOnSave() {
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> {
            Account a = invocation.getArgument(0);
            if (a.getId() == null) {
                a.setId(42L);
            }
            return a;
        });
    }

    private static void assertAmount(String expected, BigDecimal actual) {
        assertTrue(actual != null && new BigDecimal(expected).compareTo(actual) == 0,
                "expected " + expected + " but was " + actual);
    }

    private Account existing(Long id, Long loanId, String balance) {
        Account a = new Account();
        a.setId(id);
        a.setLoanId(loanId);
        a.setAccountNumber("ACCT-EXIST");
        a.setCurrentBalance(new BigDecimal(balance));
        a.setInterestRate(new BigDecimal("6.000"));
        a.setOutstandingFees(BigDecimal.ZERO);
        a.setStatus(AccountStatus.CURRENT);
        return a;
    }

    @Test
    void createAccountForLoanPublishesAccountCreatedOnce() {
        assignIdOnSave();
        when(accountRepository.findByLoanId(1001L)).thenReturn(Optional.empty());

        Account created = service.createAccountForLoan(1001L, null,
                new BigDecimal("20000.00"), new BigDecimal("6.00"), 60);

        assertEquals(AccountStatus.CURRENT, created.getStatus());
        assertAmount("20000.00", created.getCurrentBalance());
        assertAmount("20000.00", created.getOriginalBalance());

        ArgumentCaptor<AccountCreated> captor = ArgumentCaptor.forClass(AccountCreated.class);
        verify(eventPublisher).publish(captor.capture());
        AccountCreated event = captor.getValue();
        assertEquals(42L, event.getAccountId());
        assertEquals(1001L, event.getLoanId());
        assertEquals(created.getAccountNumber(), event.getAccountNumber());
        assertAmount("20000.00", event.getOriginalBalance());
        assertNull(event.getCustomerName());
    }

    @Test
    void createAccountForLoanIsIdempotentWhenAccountAlreadyExists() {
        when(accountRepository.findByLoanId(1001L))
                .thenReturn(Optional.of(existing(42L, 1001L, "18000.00")));

        Account result = service.createAccountForLoan(1001L, null,
                new BigDecimal("20000.00"), new BigDecimal("6.00"), 60);

        assertNull(result);
        verify(accountRepository, never()).save(any(Account.class));
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void applyProcessedPaymentUpdatesBalanceAndPublishesPayload() {
        Account account = existing(42L, 1001L, "10000.00");
        when(accountRepository.findByLoanId(1001L)).thenReturn(Optional.of(account));

        service.applyProcessedPayment(1001L, new BigDecimal("400.00"));

        assertAmount("9600.00", account.getCurrentBalance());

        ArgumentCaptor<AccountBalanceUpdated> captor = ArgumentCaptor.forClass(AccountBalanceUpdated.class);
        verify(eventPublisher).publish(captor.capture());
        AccountBalanceUpdated event = captor.getValue();
        assertEquals(42L, event.getAccountId());
        assertAmount("10000.00", event.getPreviousBalance());
        assertAmount("9600.00", event.getNewBalance());
        assertAmount("400.00", event.getChangeAmount());
        assertEquals("PAYMENT", event.getChangeReason());
    }

    @Test
    void applyProcessedPaymentIgnoredWhenAccountMissing() {
        when(accountRepository.findByLoanId(1001L)).thenReturn(Optional.empty());

        service.applyProcessedPayment(1001L, new BigDecimal("400.00"));

        verify(accountRepository, never()).save(any(Account.class));
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void assessDelinquencyPublishesDelinquentPayload() {
        Account account = existing(42L, 1001L, "9800.00");
        when(accountRepository.findById(42L)).thenReturn(Optional.of(account));

        service.assessDelinquency(42L, 45);

        assertEquals(AccountStatus.DELINQUENT_30, account.getStatus());
        assertEquals(45, account.getDaysPastDue());

        ArgumentCaptor<AccountDelinquent> captor = ArgumentCaptor.forClass(AccountDelinquent.class);
        verify(eventPublisher).publish(captor.capture());
        AccountDelinquent event = captor.getValue();
        assertEquals(42L, event.getAccountId());
        assertEquals(1001L, event.getLoanId());
        assertEquals(45, event.getDaysPastDue());
        assertEquals("30-59", event.getDelinquencyBucket());
        assertAmount("9800.00", event.getCurrentBalance());
    }

    @Test
    void payoffQuoteUsesAccountOwnedStateOnly() {
        Account account = existing(42L, 1001L, "10000.00");
        account.setOutstandingFees(new BigDecimal("25.00"));
        when(accountRepository.findById(42L)).thenReturn(Optional.of(account));

        Map<String, Object> quote = service.calculatePayoffQuote(42L);

        // 6% annual -> daily 6/36500; per-diem on 10000 = 1.64; ten-day = 16.40.
        assertAmount("1.64", (BigDecimal) quote.get("perDiemInterest"));
        assertAmount("16.40", (BigDecimal) quote.get("tenDayInterest"));
        assertAmount("25.00", (BigDecimal) quote.get("outstandingFees"));
        assertAmount("10041.40", (BigDecimal) quote.get("totalPayoffAmount"));
        assertEquals("ACCT-EXIST", quote.get("accountNumber"));
    }

    @Test
    void earlyTerminationZeroesBalanceAndPublishesBalanceUpdateWithoutTouchingLoan() {
        Account account = existing(42L, 1001L, "5000.00");
        when(accountRepository.findById(42L)).thenReturn(Optional.of(account));

        Map<String, Object> result = service.processEarlyTermination(42L);

        assertEquals("TERMINATED", result.get("status"));
        assertEquals(AccountStatus.EARLY_TERMINATION, account.getStatus());
        assertAmount("0.00", account.getCurrentBalance());

        ArgumentCaptor<AccountBalanceUpdated> captor = ArgumentCaptor.forClass(AccountBalanceUpdated.class);
        verify(eventPublisher).publish(captor.capture());
        assertEquals("EARLY_TERMINATION", captor.getValue().getChangeReason());
        assertAmount("5000.00", captor.getValue().getPreviousBalance());
        assertAmount("0.00", captor.getValue().getNewBalance());
    }

    @Test
    void getAccountDetailsThrowsWhenMissing() {
        when(accountRepository.findById(99L)).thenReturn(Optional.empty());
        try {
            service.getAccountDetails(99L);
            org.junit.jupiter.api.Assertions.fail("expected AccountNotFoundException");
        } catch (AccountNotFoundException expected) {
            assertTrue(expected.getMessage().contains("99"));
        }
    }
}
