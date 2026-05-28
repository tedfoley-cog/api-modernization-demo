package com.acme.autofinance.repository;

import com.acme.autofinance.model.Payment;
import com.acme.autofinance.model.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByLoanId(Long loanId);

    List<Payment> findByLoanIdAndStatus(Long loanId, PaymentStatus status);

    @Query(value = "SELECT SUM(payment_amount) FROM payments WHERE loan_id = ?1 AND status = 'COMPLETED'",
           nativeQuery = true)
    java.math.BigDecimal sumCompletedPayments(Long loanId);

    List<Payment> findByStatus(PaymentStatus status);
}
