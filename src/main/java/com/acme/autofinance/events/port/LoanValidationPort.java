package com.acme.autofinance.events.port;

import java.util.Optional;

/**
 * Anti-corruption layer between the Payment context and the Loan/Account
 * contexts. The Payment service depends on this interface instead of injecting
 * {@code LoanRepository} / {@code AccountRepository} directly, so payment
 * processing no longer reaches across domain boundaries into other contexts'
 * data.
 *
 * <p>In the target microservice world this would be backed by a synchronous API
 * call (or a locally cached read model); today it is a thin in-process adapter.
 */
public interface LoanValidationPort {

    /**
     * @return a read-only loan reference if the loan exists, otherwise empty.
     */
    Optional<LoanReference> findLoanReference(Long loanId);
}
