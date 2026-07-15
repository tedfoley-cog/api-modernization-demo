package com.acme.autofinance.payment.repository;

import com.acme.autofinance.payment.domain.AccountProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccountProjectionRepository extends JpaRepository<AccountProjection, Long> {

    Optional<AccountProjection> findByLoanId(Long loanId);
}
