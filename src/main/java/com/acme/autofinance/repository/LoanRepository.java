package com.acme.autofinance.repository;

import com.acme.autofinance.model.LoanApplication;
import com.acme.autofinance.model.LoanStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LoanRepository extends JpaRepository<LoanApplication, Long> {

    Optional<LoanApplication> findByApplicationNumber(String applicationNumber);

    List<LoanApplication> findByStatus(LoanStatus status);

    List<LoanApplication> findByDealerId(Long dealerId);

    @Query("SELECT l FROM LoanApplication l WHERE l.dealerId = ?1 AND l.status = ?2")
    List<LoanApplication> findByDealerIdAndStatus(Long dealerId, LoanStatus status);

    @Query(value = "SELECT COUNT(*) FROM loan_applications WHERE status = ?1", nativeQuery = true)
    long countByStatus(String status);
}
