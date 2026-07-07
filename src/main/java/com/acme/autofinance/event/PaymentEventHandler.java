package com.acme.autofinance.event;

import com.acme.autofinance.model.Account;
import com.acme.autofinance.model.AccountStatus;
import com.acme.autofinance.model.LoanApplication;
import com.acme.autofinance.model.LoanStatus;
import com.acme.autofinance.repository.AccountRepository;
import com.acme.autofinance.repository.LoanRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Monolith-side event handler: reacts to payment domain events.
 * Replaces the synchronous cross-domain calls that were previously inline.
 *
 * In the microservice architecture, Account Service and Loan Service would
 * each have their own event handlers consuming from Kafka topics.
 */
@Component
public class PaymentEventHandler {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventHandler.class);

    private final AccountRepository accountRepository;
    private final LoanRepository loanRepository;

    public PaymentEventHandler(AccountRepository accountRepository, LoanRepository loanRepository) {
        this.accountRepository = accountRepository;
        this.loanRepository = loanRepository;
    }

    @EventListener
    @Transactional
    public void onPaymentCompleted(PaymentCompletedEvent event) {
        log.info("Event received: PaymentCompleted for loanId={}, principal={}",
                event.getLoanId(), event.getPrincipalAmount());

        // Update account balance (was synchronous updateAccountBalance in PaymentService)
        Optional<Account> accountOpt = accountRepository.findByLoanId(event.getLoanId());
        if (accountOpt.isPresent()) {
            Account account = accountOpt.get();
            if (account.getCurrentBalance() != null && event.getPrincipalAmount() != null) {
                BigDecimal newBalance = account.getCurrentBalance().subtract(event.getPrincipalAmount());
                if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
                    newBalance = BigDecimal.ZERO;
                }
                account.setCurrentBalance(newBalance);
                account.setLastPaymentDate(event.getOccurredAt());
                account.setDaysPastDue(0);
                accountRepository.save(account);
                log.info("Account balance updated for loanId={}, newBalance={}", event.getLoanId(), newBalance);
            }
        }

        // Check if loan is paid off (was synchronous updateLoanAfterPayment in PaymentService)
        if (event.getTotalPaidToDate() != null) {
            Optional<LoanApplication> loanOpt = loanRepository.findById(event.getLoanId());
            if (loanOpt.isPresent()) {
                LoanApplication loan = loanOpt.get();
                if (loan.getApprovedAmount() != null &&
                        event.getTotalPaidToDate().compareTo(loan.getApprovedAmount()) >= 0) {
                    loan.setStatus(LoanStatus.PAID_OFF);
                    loanRepository.save(loan);
                    log.info("Loan paid off: loanId={}", event.getLoanId());

                    // Close account
                    if (accountOpt.isPresent()) {
                        Account account = accountOpt.get();
                        account.setStatus(AccountStatus.PAID_IN_FULL);
                        account.setCurrentBalance(BigDecimal.ZERO);
                        accountRepository.save(account);
                    }
                }
            }
        }
    }

    @EventListener
    public void onLateFeeAssessed(LateFeeAssessedEvent event) {
        log.info("Event received: LateFeeAssessed for loanId={}, fee={}, daysPastDue={}",
                event.getLoanId(), event.getFeeAmount(), event.getDaysPastDue());
        // Late fee record is already created in the payments table by PaymentService.
        // Account Service would track outstanding fees separately in production.
    }
}
