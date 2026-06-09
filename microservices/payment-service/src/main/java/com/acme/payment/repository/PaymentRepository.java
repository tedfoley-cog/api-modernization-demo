package com.acme.payment.repository;

import com.acme.payment.domain.Payment;
import com.acme.payment.domain.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository over the payment service's own H2 schema.
 * No tables are shared with other bounded contexts.
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByLoanId(Long loanId);

    List<Payment> findByLoanIdAndStatus(Long loanId, PaymentStatus status);

    List<Payment> findByStatus(PaymentStatus status);

    @Query("SELECT COALESCE(SUM(p.paymentAmount), 0) FROM Payment p "
            + "WHERE p.loanId = ?1 AND p.status = com.acme.payment.domain.PaymentStatus.COMPLETED")
    java.math.BigDecimal sumCompletedPayments(Long loanId);
}
