package com.acme.autofinance.repository;

import com.acme.autofinance.model.Dealer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DealerRepository extends JpaRepository<Dealer, Long> {

    Optional<Dealer> findByDealerCode(String dealerCode);
}
