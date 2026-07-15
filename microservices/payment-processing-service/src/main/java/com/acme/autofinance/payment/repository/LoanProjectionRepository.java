package com.acme.autofinance.payment.repository;

import com.acme.autofinance.payment.domain.LoanProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LoanProjectionRepository extends JpaRepository<LoanProjection, Long> {
}
