package com.acme.autofinance.repository;

import com.acme.autofinance.model.DealPackage;
import com.acme.autofinance.model.DealStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DealPackageRepository extends JpaRepository<DealPackage, Long> {

    Optional<DealPackage> findByDealNumber(String dealNumber);

    List<DealPackage> findByDealerId(Long dealerId);

    List<DealPackage> findByStatus(DealStatus status);
}
