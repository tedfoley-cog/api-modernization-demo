package com.acme.autofinance.reporting.repository;

import com.acme.autofinance.reporting.domain.AccountReportProjection;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountReportRepository extends JpaRepository<AccountReportProjection, Long> {
}
