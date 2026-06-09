package com.acme.payment.query;

import com.acme.payment.domain.PaymentStatus;
import com.acme.payment.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Read side of the CQRS split. Exposes query operations only and returns immutable
 * {@link PaymentView} projections rather than JPA entities.
 */
@Service
public class PaymentQueryService {

    private final PaymentRepository paymentRepository;

    public PaymentQueryService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Transactional(readOnly = true)
    public List<PaymentView> getPaymentHistory(Long loanId) {
        return paymentRepository.findByLoanId(loanId).stream()
                .map(PaymentView::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PaymentView> getPendingPayments() {
        return paymentRepository.findByStatus(PaymentStatus.PENDING).stream()
                .map(PaymentView::from)
                .collect(Collectors.toList());
    }
}
