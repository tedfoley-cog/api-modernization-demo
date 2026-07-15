package com.acme.autofinance.reporting.repository;

import com.acme.autofinance.reporting.domain.LoanReportProjection;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoanReportRepository extends JpaRepository<LoanReportProjection, Long> {
}
