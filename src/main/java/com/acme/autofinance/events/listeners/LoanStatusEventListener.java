package com.acme.autofinance.events.listeners;

import com.acme.autofinance.events.DomainEventPublisher;
import com.acme.autofinance.events.LoanPayoffEvent;
import com.acme.autofinance.events.PaymentCompletedEvent;
import com.acme.autofinance.model.LoanApplication;
import com.acme.autofinance.model.LoanStatus;
import com.acme.autofinance.repository.LoanRepository;
import com.acme.autofinance.repository.PaymentRepository;
import com.acme.autofinance.service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Loan Origination context reaction to a completed payment.
 *
 * <p>Owns the logic previously embedded in {@code PaymentService.updateLoanAfterPayment()}:
 * checks whether total completed payments satisfy the loan and, if so, marks it
 * {@code PAID_OFF}, closes the account, and emits a {@link LoanPayoffEvent}.
 */
@Component
public class LoanStatusEventListener {

    private final LoanRepository loanRepository;
    private final PaymentRepository paymentRepository;
    private final AccountService accountService;
    private final DomainEventPublisher eventPublisher;

    @Autowired
    public LoanStatusEventListener(LoanRepository loanRepository,
                                   PaymentRepository paymentRepository,
                                   AccountService accountService,
                                   DomainEventPublisher eventPublisher) {
        this.loanRepository = loanRepository;
        this.paymentRepository = paymentRepository;
        this.accountService = accountService;
        this.eventPublisher = eventPublisher;
    }

    @EventListener
    @Transactional
    public void on(PaymentCompletedEvent event) {
        Optional<LoanApplication> loanOpt = loanRepository.findById(event.getLoanId());
        if (!loanOpt.isPresent()) {
            return;
        }
        LoanApplication loan = loanOpt.get();

        BigDecimal totalPaid = paymentRepository.sumCompletedPayments(loan.getId());
        if (totalPaid == null || loan.getApprovedAmount() == null) {
            return;
        }

        if (totalPaid.compareTo(loan.getApprovedAmount()) >= 0) {
            loan.setStatus(LoanStatus.PAID_OFF);
            loanRepository.save(loan);
            // Closing the account stays a same-context concern of Account Servicing.
            accountService.closeAccount(loan.getId());
            eventPublisher.publish(new LoanPayoffEvent(loan.getId(), totalPaid));
        }
    }
}
