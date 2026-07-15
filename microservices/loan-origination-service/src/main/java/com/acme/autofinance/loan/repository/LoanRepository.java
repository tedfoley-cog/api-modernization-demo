package com.acme.autofinance.loan.repository;

import com.acme.autofinance.loan.domain.LoanApplication;
import com.acme.autofinance.loan.domain.LoanStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Persistence for loan applications. Backed by the loan-origination service's own
 * H2 schema; it never touches account, payment, or dealer tables. Derived queries
 * only — no raw reporting SQL lives here.
 */
@Repository
public interface LoanRepository extends JpaRepository<LoanApplication, Long> {

    Optional<LoanApplication> findByApplicationNumber(String applicationNumber);

    List<LoanApplication> findByStatus(LoanStatus status);

    List<LoanApplication> findByDealerId(Long dealerId);

    List<LoanApplication> findByDealerIdAndStatus(Long dealerId, LoanStatus status);
}
