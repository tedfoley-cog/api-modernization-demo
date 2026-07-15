package com.acme.autofinance.dealer.repository;

import com.acme.autofinance.dealer.domain.DealLoanProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DealLoanProjectionRepository extends JpaRepository<DealLoanProjection, Long> {

    Optional<DealLoanProjection> findByApplicationNumber(String applicationNumber);

    List<DealLoanProjection> findByDealerId(Long dealerId);
}
