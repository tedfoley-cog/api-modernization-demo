package com.acme.autofinance.events.listeners;

import com.acme.autofinance.events.AccountBalanceUpdatedEvent;
import com.acme.autofinance.events.DomainEventPublisher;
import com.acme.autofinance.events.PaymentCompletedEvent;
import com.acme.autofinance.model.Account;
import com.acme.autofinance.repository.AccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Optional;

/**
 * Account Servicing context reaction to a completed payment.
 *
 * <p>Owns the logic previously embedded in {@code PaymentService.updateAccountBalance()}:
 * applies the principal portion of the payment to the account's outstanding
 * balance and clears the days-past-due counter.
 */
@Component
public class AccountBalanceEventListener {

    private final AccountRepository accountRepository;
    private final DomainEventPublisher eventPublisher;

    @Autowired
    public AccountBalanceEventListener(AccountRepository accountRepository, DomainEventPublisher eventPublisher) {
        this.accountRepository = accountRepository;
        this.eventPublisher = eventPublisher;
    }

    @EventListener
    @Transactional
    public void on(PaymentCompletedEvent event) {
        Optional<Account> accountOpt = accountRepository.findByLoanId(event.getLoanId());
        if (!accountOpt.isPresent()) {
            return;
        }
        Account account = accountOpt.get();
        if (account.getCurrentBalance() == null || event.getPrincipalAmount() == null) {
            return;
        }

        BigDecimal newBalance = account.getCurrentBalance().subtract(event.getPrincipalAmount());
        if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
            newBalance = BigDecimal.ZERO;
        }
        account.setCurrentBalance(newBalance);
        account.setLastPaymentDate(new Date());
        account.setDaysPastDue(0);
        accountRepository.save(account);

        eventPublisher.publish(new AccountBalanceUpdatedEvent(
                event.getLoanId(), event.getPaymentId(), newBalance));
    }
}
