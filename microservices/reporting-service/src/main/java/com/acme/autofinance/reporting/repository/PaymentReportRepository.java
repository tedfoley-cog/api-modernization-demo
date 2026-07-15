package com.acme.autofinance.reporting.repository;

import com.acme.autofinance.reporting.domain.PaymentReportProjection;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentReportRepository extends JpaRepository<PaymentReportProjection, Long> {
}
