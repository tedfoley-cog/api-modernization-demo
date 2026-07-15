package com.acme.autofinance.loan.service;

import com.acme.autofinance.events.CreditDecisionMade;
import com.acme.autofinance.events.DomainEvent;
import com.acme.autofinance.events.LoanApplicationSubmitted;
import com.acme.autofinance.events.LoanFunded;
import com.acme.autofinance.loan.domain.LoanApplication;
import com.acme.autofinance.loan.domain.LoanStatus;
import com.acme.autofinance.loan.repository.LoanRepository;
import com.acme.autofinance.messaging.DomainEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoanServiceTest {

    @Mock private LoanRepository loanRepository;
    @Mock private DomainEventPublisher eventPublisher;

    private LoanService service;

    @BeforeEach
    void setUp() {
        service = new LoanService(loanRepository, eventPublisher);
    }

    private void assignIdOnSave(Long id) {
        when(loanRepository.save(any(LoanApplication.class))).thenAnswer(invocation -> {
            LoanApplication a = invocation.getArgument(0);
            if (a.getId() == null) {
                a.setId(id);
            }
            return a;
        });
    }

    private static void assertAmount(String expected, BigDecimal actual) {
        assertTrue(actual != null && new BigDecimal(expected).compareTo(actual) == 0,
                "expected " + expected + " but was " + actual);
    }

    private LoanApplication newApplication(String name, String amount, Integer creditScore) {
        LoanApplication a = new LoanApplication();
        a.setApplicantName(name);
        a.setRequestedAmount(new BigDecimal(amount));
        a.setVehicleVin("1FTFW1E50NFA10001");
        a.setDealerId(7L);
        a.setCreditScore(creditScore);
        return a;
    }

    @Test
    void createApplicationPublishesSubmittedAndDecisionAndApproves() {
        assignIdOnSave(42L);

        LoanApplication result = service.createApplication(newApplication("Jordan Rivera", "30000.00", 742));

        assertEquals(LoanStatus.APPROVED, result.getStatus());
        assertAmount("30000.00", result.getApprovedAmount());
        assertAmount("3.99", result.getInterestRate());

        ArgumentCaptor<DomainEvent> captor = ArgumentCaptor.forClass(DomainEvent.class);
        verify(eventPublisher, org.mockito.Mockito.times(2)).publish(captor.capture());
        List<DomainEvent> events = captor.getAllValues();

        LoanApplicationSubmitted submitted = (LoanApplicationSubmitted) events.get(0);
        assertEquals(42L, submitted.getApplicationId());
        assertEquals("Jordan Rivera", submitted.getApplicantName());
        assertEquals("1FTFW1E50NFA10001", submitted.getVehicleVin());
        assertEquals(7L, submitted.getDealerId());
        assertAmount("30000.00", submitted.getRequestedAmount());
        assertTrue(submitted.getApplicationNumber().startsWith("LN-"));

        CreditDecisionMade decision = (CreditDecisionMade) events.get(1);
        assertEquals(42L, decision.getApplicationId());
        assertEquals("APPROVED", decision.getDecision());
        assertEquals(742, decision.getCreditScore());
        assertEquals("A", decision.getRiskTier());
        assertAmount("30000.00", decision.getApprovedAmount());
        assertAmount("3.99", decision.getOfferedRate());
    }

    @Test
    void createApplicationDeclinesLowCreditScore() {
        assignIdOnSave(43L);

        LoanApplication result = service.createApplication(newApplication("Morgan Patel", "22000.00", 500));

        assertEquals(LoanStatus.DECLINED, result.getStatus());

        ArgumentCaptor<DomainEvent> captor = ArgumentCaptor.forClass(DomainEvent.class);
        verify(eventPublisher, org.mockito.Mockito.times(2)).publish(captor.capture());
        CreditDecisionMade decision = (CreditDecisionMade) captor.getAllValues().get(1);
        assertEquals("DECLINED", decision.getDecision());
        assertEquals("E", decision.getRiskTier());
    }

    @Test
    void fundLoanPublishesLoanFundedAndActivates() {
        LoanApplication approved = newApplication("Jordan Rivera", "30000.00", 742);
        approved.setId(42L);
        approved.setApplicationNumber("LN-0001");
        approved.setStatus(LoanStatus.APPROVED);
        approved.setApprovedAmount(new BigDecimal("30000.00"));
        approved.setInterestRate(new BigDecimal("3.99"));
        approved.setTermMonths(72);
        when(loanRepository.findById(42L)).thenReturn(Optional.of(approved));
        when(loanRepository.save(any(LoanApplication.class))).thenAnswer(i -> i.getArgument(0));

        LoanApplication funded = service.fundLoan(42L);

        assertEquals(LoanStatus.ACTIVE, funded.getStatus());

        ArgumentCaptor<LoanFunded> captor = ArgumentCaptor.forClass(LoanFunded.class);
        verify(eventPublisher).publish(captor.capture());
        LoanFunded event = captor.getValue();
        assertEquals(42L, event.getLoanId());
        assertEquals("LN-0001", event.getApplicationNumber());
        assertAmount("30000.00", event.getApprovedAmount());
        assertAmount("3.99", event.getInterestRate());
        assertEquals(72, event.getTermMonths());
    }

    @Test
    void fundLoanRejectsNonApprovedLoan() {
        LoanApplication submitted = newApplication("Jordan Rivera", "30000.00", 742);
        submitted.setId(42L);
        submitted.setStatus(LoanStatus.SUBMITTED);
        when(loanRepository.findById(42L)).thenReturn(Optional.of(submitted));

        assertThrows(IllegalStateException.class, () -> service.fundLoan(42L));
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void updateTermsRecalculatesMonthlyPayment() {
        LoanApplication approved = newApplication("Jordan Rivera", "30000.00", 742);
        approved.setId(42L);
        approved.setStatus(LoanStatus.APPROVED);
        approved.setApprovedAmount(new BigDecimal("30000.00"));
        approved.setInterestRate(new BigDecimal("3.99"));
        approved.setTermMonths(60);
        when(loanRepository.findById(42L)).thenReturn(Optional.of(approved));
        when(loanRepository.save(any(LoanApplication.class))).thenAnswer(i -> i.getArgument(0));

        LoanApplication result = service.updateTerms(42L, new BigDecimal("30000.00"),
                new BigDecimal("3.99"), 72);

        assertEquals(72, result.getTermMonths());
        assertTrue(result.getMonthlyPayment() != null
                && result.getMonthlyPayment().compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    void getApplicationThrowsWhenMissing() {
        when(loanRepository.findById(99L)).thenReturn(Optional.empty());
        LoanNotFoundException ex = assertThrows(LoanNotFoundException.class,
                () -> service.getApplication(99L));
        assertTrue(ex.getMessage().contains("99"));
    }

    @Test
    void createApplicationFromDealSubmissionDerivesFinancedAmount() {
        assignIdOnSave(44L);

        LoanApplication result = service.createApplicationFromDealSubmission(
                "DEAL-9", 7L, "1FTFW1E50NFA10001",
                new BigDecimal("35000.00"), new BigDecimal("5000.00"), new BigDecimal("2000.00"));

        // 35000 - 5000 - 2000 = 28000 financed
        assertAmount("28000.00", result.getRequestedAmount());
        assertEquals(7L, result.getDealerId());
        assertEquals("DEALER SUBMISSION DEAL-9", result.getApplicantName());

        ArgumentCaptor<DomainEvent> captor = ArgumentCaptor.forClass(DomainEvent.class);
        verify(eventPublisher, org.mockito.Mockito.times(2)).publish(captor.capture());
        assertTrue(captor.getAllValues().get(0) instanceof LoanApplicationSubmitted);
        assertTrue(captor.getAllValues().get(1) instanceof CreditDecisionMade);
    }

    @Test
    void projectPaymentProcessedMarksPaidOffWhenBalanceZero() {
        LoanApplication active = newApplication("Jordan Rivera", "30000.00", 742);
        active.setId(42L);
        active.setStatus(LoanStatus.ACTIVE);
        when(loanRepository.findById(42L)).thenReturn(Optional.of(active));
        when(loanRepository.save(any(LoanApplication.class))).thenAnswer(i -> i.getArgument(0));

        boolean projected = service.projectPaymentProcessed(42L, BigDecimal.ZERO);

        assertTrue(projected);
        assertEquals(LoanStatus.PAID_OFF, active.getStatus());
    }

    @Test
    void projectPaymentProcessedIgnoresUnknownLoan() {
        when(loanRepository.findById(77L)).thenReturn(Optional.empty());

        boolean projected = service.projectPaymentProcessed(77L, BigDecimal.ZERO);

        assertTrue(!projected);
        verify(loanRepository, never()).save(any());
    }
}
