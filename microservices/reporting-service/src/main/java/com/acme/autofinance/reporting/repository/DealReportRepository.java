package com.acme.autofinance.reporting.repository;

import com.acme.autofinance.reporting.domain.DealReportProjection;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DealReportRepository extends JpaRepository<DealReportProjection, String> {
}
