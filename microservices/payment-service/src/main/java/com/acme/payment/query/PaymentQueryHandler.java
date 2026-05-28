package com.acme.payment.query;

import com.acme.payment.model.Payment;
import com.acme.payment.model.PaymentStatus;
import com.acme.payment.repository.PaymentRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * CQRS read side: handles all payment queries.
 * Separated from command handling to allow independent scaling and optimization.
 */
@Service
public class PaymentQueryHandler {

    private final PaymentRepository paymentRepository;

    public PaymentQueryHandler(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    public Payment getPaymentById(Long id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment not found: " + id));
    }

    public List<Payment> getPaymentHistory(Long loanId) {
        return paymentRepository.findByLoanId(loanId);
    }

    public List<Payment> getPendingPayments() {
        return paymentRepository.findByStatus(PaymentStatus.PENDING);
    }

    public BigDecimal getTotalPaidForLoan(Long loanId) {
        BigDecimal total = paymentRepository.sumCompletedPayments(loanId);
        return total != null ? total : BigDecimal.ZERO;
    }
}
