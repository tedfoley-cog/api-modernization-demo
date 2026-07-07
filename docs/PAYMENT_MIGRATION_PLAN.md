# Payment Bounded Context Extraction Plan

Migration plan for decomposing the auto-finance monolith's Payment Processing
bounded context into a standalone, event-driven Spring Boot microservice.

---

## 1. Coupling Analysis

### 1.1 God Service Inventory (`LoanService.java`, 478 lines)

| Bounded Context | Methods in LoanService | Lines | Coupling |
|---|---|---|---|
| Loan Origination | `createApplication`, `getApplication`, `getByApplicationNumber`, `getByStatus`, `updateTerms`, `fundLoan` | 73-202 | Core domain, stays |
| Credit Decisioning | `performCreditCheck` | 208-256 | Inline, synchronous; should be external call |
| Payment Math | `calculateMonthlyPayment`, `calculatePayoffAmount` | 262-289 | **Extract** to Payment service |
| Dealer Settlement | `updateDealPackageForApproval` | 295-317 | Should move to DealerService |
| Portfolio Reporting | `getPortfolioSummary`, `getDelinquencyReport`, status breakdowns | 323-417 | Direct SQL across all tables; replace with CQRS |
| Batch/EOD | `runEndOfDayProcessing` | 424-468 | Cross-domain loop; decompose into events |

### 1.2 Cross-Domain Dependencies

```
LoanService ──@Autowired──> PaymentService.assessLateFee()
LoanService ──@Autowired──> AccountService (via accountRepository.findByLoanId)
LoanService ──JdbcTemplate──> payments, accounts, loan_applications tables
PaymentService ──@Autowired──> LoanRepository.findById()
PaymentService ──@Autowired──> AccountRepository.findByLoanId()
PaymentService ──@Autowired──> AccountService.closeAccount()
AccountService ──@Autowired──> LoanRepository, PaymentRepository
DealerService ──@Autowired──> LoanService.createApplication()
ReportService ──JdbcTemplate──> all tables (loans, payments, accounts, dealers)
```

### 1.3 Shared Database Tables

All five domains share the H2 database `autofinancedb`:
- `loan_applications` — Loan domain (primary), read by Payment, Account, Dealer, Report
- `payments` — Payment domain (primary), read by Account, Loan, Report
- `accounts` — Account domain (primary), read by Payment, Loan, Report
- `dealers` / `deal_packages` — Dealer domain (primary), read by Loan, Report

### 1.4 Payment Domain Boundary

**Entities owned by Payment:**
- `Payment` (table: `payments`)
- `PaymentStatus`, `PaymentMethod` enums

**Logic to extract from LoanService:**
- `calculateMonthlyPayment(principal, rate, termMonths)` (lines 262-280)
- `calculatePayoffAmount(loan)` (lines 282-289)

**Logic already in PaymentService (moves to microservice):**
- `submitPayment`, `processAchPayment`, `allocatePayment`
- `assessLateFee`, `getPaymentHistory`, `processBatchPayments`, `getPendingPayments`
- `updateAccountBalance` (becomes event-driven)
- `updateLoanAfterPayment` (becomes event-driven)

---

## 2. CONVERT Pipeline

Following `docs/IMPLEMENTATION_PLAN.md` lines 94-98:
**EXTRACT -> EVENTS -> SERVICE -> TESTS**

### Phase 1: EXTRACT (Bounded Context Isolation)

1. Create `microservices/payment-service/` as a standalone Spring Boot module.
2. Copy `Payment` entity, `PaymentStatus`, `PaymentMethod` enums.
3. Move payment calculation logic (`calculateMonthlyPayment`, `calculatePayoffAmount`)
   into the microservice's domain.
4. Payment service owns the `payments` table exclusively.
5. Keep monolith's `PaymentService` as a **thin facade** that delegates to events
   (strangler-fig pattern).

### Phase 2: EVENTS (Domain Event Definitions)

Replace synchronous cross-domain calls with domain events:

| Synchronous Call | Domain Event | Publisher | Subscribers |
|---|---|---|---|
| `PaymentService.updateAccountBalance()` | `PaymentReceived` | Payment Service | Account Service |
| `PaymentService.updateLoanAfterPayment()` | `PaymentCompleted` | Payment Service | Loan Service |
| `PaymentService.assessLateFee()` (called from `LoanService.runEndOfDayProcessing`) | `LateFeeAssessed` | Payment Service | Account Service |
| `LoanService.createApplication()` inline account creation | `LoanApproved` | Loan Service | Account Service |
| `LoanService.runEndOfDayProcessing()` loan status update | `AccountDelinquent` | Account Service | Loan Service |

Event bus: **In-memory `ApplicationEventPublisher`** simulating Kafka.

> **Production note:** A real deployment requires Apache Kafka (or equivalent) with
> delivery guarantees, ordering by partition key (`loanId`), and durability via
> replication. The in-memory bus is demo-only. (ref: `IMPLEMENTATION_PLAN.md` line 123)

### Phase 3: SERVICE (Microservice Build)

```
microservices/payment-service/
  pom.xml
  src/main/java/com/acme/payment/
    PaymentServiceApplication.java       # Spring Boot main
    config/
      EventBusConfig.java                # In-memory event bus setup
    domain/
      model/
        Payment.java                     # Rich domain entity
        PaymentStatus.java
        PaymentMethod.java
        PaymentAllocation.java           # Value object: principal/interest/fee split
      event/
        PaymentReceived.java             # Domain event
        PaymentCompleted.java
        PaymentFailed.java
        LateFeeAssessed.java
        LoanApprovedEvent.java           # Consumed from Loan context
      repository/
        PaymentRepository.java
    service/
      PaymentCommandService.java         # CQRS write side
      PaymentQueryService.java           # CQRS read side
      PaymentCalculationService.java     # Monthly payment, payoff math
      LateFeeService.java                # Late fee assessment
    controller/
      PaymentController.java             # REST API
    saga/
      PaymentSagaOrchestrator.java       # Saga for distributed tx
  src/main/resources/
    application.properties               # Separate datasource
    schema.sql                           # Payment-owned tables only
    data.sql                             # Payment seed data
  src/test/java/
    ...                                  # Integration tests
```

### Phase 4: TESTS (Integration Tests)

1. **Payment submission flow**: submit payment -> event published -> allocation correct
2. **ACH processing**: submit ACH payment -> `PaymentReceived` event -> verify status transitions
3. **Late fee assessment**: trigger late fee -> `LateFeeAssessed` event -> verify fee record
4. **Payment calculation**: `calculateMonthlyPayment` and `calculatePayoffAmount` accuracy
5. **Batch processing**: submit batch -> verify individual results
6. **Event publishing**: verify all domain events are published with correct payloads
7. **Saga compensation**: simulate failure -> verify rollback behavior

---

## 3. Distributed Transaction Strategy

### Before (Monolith)
```java
@Transactional  // Single DB transaction across all domains
public void processAchPayment(Payment payment) {
    paymentRepository.save(payment);       // Payment domain
    updateAccountBalance(payment);          // Account domain
    updateLoanAfterPayment(payment);        // Loan domain
}
```

### After (Saga Pattern)
```
PaymentSaga:
  1. Mark payment PROCESSING          (Payment DB)
  2. Publish PaymentReceived event
  3. Account Service: update balance   (Account DB)
     - On failure: publish AccountUpdateFailed
  4. Loan Service: check payoff        (Loan DB)
     - On failure: publish LoanUpdateFailed

Compensation:
  - AccountUpdateFailed -> reverse payment to FAILED, refund
  - LoanUpdateFailed -> reverse payment to FAILED, restore account balance
```

---

## 4. CQRS Read Model

### Before
`LoanService.getPortfolioSummary()` issues raw SQL JOINs across `loan_applications`,
`accounts`, and `payments` tables.

### After
- Payment service maintains a **payment summary projection** updated by events.
- Reporting queries the projection API instead of direct SQL.
- The monolith's `ReportService` calls the Payment microservice's query endpoint
  for payment-related metrics, composing the portfolio view from multiple services.

---

## 5. Externalized Business Rules

| Rule | Current Location | New Owner |
|---|---|---|
| Late fee flat amount ($25) | `PaymentService` line 44 | Payment service config |
| Late fee percentage (5%) | `PaymentService` line 45 | Payment service config |
| Max late fee cap ($50) | `PaymentService` line 46 | Payment service config |
| Payment allocation order (fees->interest->principal) | `PaymentService.allocatePayment()` | Payment service domain |
| Credit score thresholds (720/680/620/560) | `LoanService.performCreditCheck()` | Stays in Loan (future: Credit service) |
| Offered rates (3.99/5.49/8.99/14.99) | `LoanService.performCreditCheck()` | Stays in Loan (future: Credit service) |
| Dealer reserve rate (1.50% default) | `LoanService.updateDealPackageForApproval()` | Future: Dealer service |

---

## 6. Migration Sequence (Strangler-Fig)

1. **Phase A** (this PR): Create Payment microservice with events and CQRS.
   Monolith `PaymentService` becomes a thin facade publishing events.
   LoanService delegates payment math to the new calculation service.
   All existing endpoints remain functional.

2. **Phase B** (future): Extract Account bounded context similarly.

3. **Phase C** (future): Extract Credit Decisioning as external service.

4. **Phase D** (future): Extract Dealer/Reporting contexts. Remove shared database.

---

## 7. API Contract Changes

### New Payment Microservice Endpoints
- `POST /api/v2/payments` — submit payment (returns confirmation + publishes event)
- `GET /api/v2/payments/loan/{loanId}` — payment history
- `POST /api/v2/payments/batch` — batch submission
- `GET /api/v2/payments/pending` — pending payments
- `GET /api/v2/payments/summary` — CQRS read model (for reporting)
- `POST /api/v2/payments/calculate-monthly` — payment math
- `POST /api/v2/payments/calculate-payoff` — payoff calculation

### Monolith Changes (Strangler)
- Existing `/api/payments/*` endpoints remain, delegating to events.
- `LoanService.calculateMonthlyPayment()` delegates to shared calculation utility.
- `LoanService.runEndOfDayProcessing()` late-fee path publishes events instead of
  synchronous `paymentService.assessLateFee()`.
