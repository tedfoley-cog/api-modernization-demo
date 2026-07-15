package com.acme.autofinance.account.repository;

import com.acme.autofinance.account.domain.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, String> {
}
