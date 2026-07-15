package com.acme.autofinance.reporting.repository;

import com.acme.autofinance.reporting.domain.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, String> {
}
