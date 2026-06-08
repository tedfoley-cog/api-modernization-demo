package com.acme.payment.query;

import com.acme.payment.domain.PaymentStatus;
import com.acme.payment.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Read side of the Payment Processing CQRS split. Returns immutable {@link PaymentView}
 * projections and performs no writes.
 */
@Service
@Transactional(readOnly = true)
public class PaymentQueryService {

    private final PaymentRepository paymentRepository;

    public PaymentQueryService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    public List<PaymentView> getPaymentHistory(Long loanId) {
        return paymentRepository.findByLoanIdOrderByReceivedAtDesc(loanId).stream()
                .map(PaymentView::from)
                .collect(Collectors.toList());
    }

    public List<PaymentView> getPendingPayments() {
        return paymentRepository.findByStatus(PaymentStatus.PENDING).stream()
                .map(PaymentView::from)
                .collect(Collectors.toList());
    }
}
