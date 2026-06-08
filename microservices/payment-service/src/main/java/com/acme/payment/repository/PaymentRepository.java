package com.acme.payment.repository;

import com.acme.payment.domain.Payment;
import com.acme.payment.domain.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByLoanId(Long loanId);

    List<Payment> findByLoanIdAndStatus(Long loanId, PaymentStatus status);

    List<Payment> findByStatus(PaymentStatus status);
}
