package com.acme.autofinance.events.port;

import com.acme.autofinance.model.Account;
import com.acme.autofinance.model.LoanApplication;
import com.acme.autofinance.repository.AccountRepository;
import com.acme.autofinance.repository.LoanRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * In-process implementation of {@link LoanValidationPort}. Lives outside the
 * Payment service so that the repositories it bridges stay an implementation
 * detail of the anti-corruption layer rather than a direct dependency of
 * {@code PaymentService}.
 */
@Component
public class LoanValidationAdapter implements LoanValidationPort {

    private final LoanRepository loanRepository;
    private final AccountRepository accountRepository;

    @Autowired
    public LoanValidationAdapter(LoanRepository loanRepository, AccountRepository accountRepository) {
        this.loanRepository = loanRepository;
        this.accountRepository = accountRepository;
    }

    @Override
    public Optional<LoanReference> findLoanReference(Long loanId) {
        Optional<LoanApplication> loanOpt = loanRepository.findById(loanId);
        if (!loanOpt.isPresent()) {
            return Optional.empty();
        }
        LoanApplication loan = loanOpt.get();
        Optional<Account> accountOpt = accountRepository.findByLoanId(loanId);
        return Optional.of(new LoanReference(
                loan.getId(),
                loan.getInterestRate(),
                loan.getApprovedAmount(),
                accountOpt.map(Account::getCurrentBalance).orElse(null)));
    }
}
