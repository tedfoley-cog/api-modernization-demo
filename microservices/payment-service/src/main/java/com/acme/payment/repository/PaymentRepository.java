package com.acme.payment.repository;

import com.acme.payment.model.Payment;
import com.acme.payment.model.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByLoanId(Long loanId);

    List<Payment> findByLoanIdAndStatus(Long loanId, PaymentStatus status);

    @Query(value = "SELECT COALESCE(SUM(payment_amount), 0) FROM payments " +
           "WHERE loan_id = ?1 AND status = 'COMPLETED'", nativeQuery = true)
    BigDecimal sumCompletedPayments(Long loanId);

    List<Payment> findByStatus(PaymentStatus status);
}
