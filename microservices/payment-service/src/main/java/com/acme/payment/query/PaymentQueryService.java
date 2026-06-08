package com.acme.payment.query;

import com.acme.payment.domain.PaymentStatus;
import com.acme.payment.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Query (read) side. Returns {@link PaymentView} read models and never mutates
 * state or publishes events.
 */
@Service
@Transactional(readOnly = true)
public class PaymentQueryService {

    private final PaymentRepository paymentRepository;

    public PaymentQueryService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    public List<PaymentView> historyForLoan(Long loanId) {
        return paymentRepository.findByLoanId(loanId).stream()
                .map(PaymentView::from)
                .collect(Collectors.toList());
    }

    public List<PaymentView> pendingPayments() {
        return paymentRepository.findByStatus(PaymentStatus.PENDING).stream()
                .map(PaymentView::from)
                .collect(Collectors.toList());
    }
}
