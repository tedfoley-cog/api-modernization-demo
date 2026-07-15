package com.acme.autofinance.account.repository;

import com.acme.autofinance.account.domain.Account;
import com.acme.autofinance.account.domain.AccountStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByAccountNumber(String accountNumber);

    Optional<Account> findByLoanId(Long loanId);

    List<Account> findByStatus(AccountStatus status);

    @Query(value = "SELECT * FROM accounts WHERE days_past_due > ?1", nativeQuery = true)
    List<Account> findDelinquentAccounts(int daysPastDue);
}
