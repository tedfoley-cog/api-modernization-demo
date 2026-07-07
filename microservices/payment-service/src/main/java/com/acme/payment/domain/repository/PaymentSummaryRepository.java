package com.acme.payment.domain.repository;

import com.acme.payment.domain.model.PaymentSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentSummaryRepository extends JpaRepository<PaymentSummary, Long> {
}
