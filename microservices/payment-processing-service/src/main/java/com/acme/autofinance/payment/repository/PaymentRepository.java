package com.acme.autofinance.payment.repository;

import com.acme.autofinance.payment.domain.Payment;
import com.acme.autofinance.payment.domain.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByLoanId(Long loanId);

    List<Payment> findByLoanIdAndStatus(Long loanId, PaymentStatus status);

    @Query(value = "SELECT SUM(payment_amount) FROM payments WHERE loan_id = ?1 AND status = 'COMPLETED'",
           nativeQuery = true)
    BigDecimal sumCompletedPayments(Long loanId);

    List<Payment> findByStatus(PaymentStatus status);
}
