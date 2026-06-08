# Payment Service

A standalone Spring Boot microservice that extracts **Payment Processing** out of
the auto-finance monolith. It is event-driven and uses **CQRS**, decoupling
payment processing from the loan and account domains.

- Java 17, Spring Boot 2.7.18, package `com.acme.payment`
- Builds and runs independently (it is **not** a Maven module of the root pom)
- In-memory H2 (`jdbc:h2:mem:paymentdb`); no shared schema with the monolith
- Does **not** import or depend on `com.acme.autofinance.*`

## Run

```bash
mvn -f microservices/payment-service/pom.xml spring-boot:run
# package
mvn -f microservices/payment-service/pom.xml -DskipTests package
# tests
mvn -f microservices/payment-service/pom.xml test
```

## REST surface (same as the monolith)

| Method | Path                          | Description                       |
|--------|-------------------------------|-----------------------------------|
| POST   | `/api/payments`               | Submit a payment                  |
| GET    | `/api/payments/loan/{loanId}` | Payment history for a loan        |
| POST   | `/api/payments/batch`         | Submit a batch of payments        |
| GET    | `/api/payments/pending`       | List pending payments             |

Example submit body:

```json
{
  "loanId": 1001,
  "paymentAmount": 500.00,
  "paymentMethod": "ACH",
  "achRoutingNumber": "021000021",
  "achAccountNumber": "123456789",
  "outstandingBalance": 10000.00,
  "monthlyInterestRate": 0.005,
  "outstandingFees": 25.00
}
```

## Design

### Before (monolith) vs. after (this service)

**Before** — `PaymentService.submitPayment()` was tightly coupled to other domains:

- looked up the loan via `loanRepository` (cross-domain read)
- read the account balance via `accountRepository` during allocation
- on ACH, **synchronously** mutated the account balance and loan status and
  even called `accountService.closeAccount(...)`

These synchronous cross-domain writes make the service hard to extract, scale or
deploy on its own.

**After** — payment processing owns only payment state and communicates outward
by **publishing domain events**:

```
submit(command)                         assessLateFee(command)
  Payment.submit(...)                      Payment.lateFeeCharge(...)
  Payment.allocate(balance, rate, fees)    publish LateFeesAssessed
  publish PaymentReceived
  publish PaymentAllocated
  if ACH:
    Payment.settleAch()
    publish PaymentProcessed
```

The loan/account domains react to these events downstream instead of being
called synchronously. The allocation inputs it used to *fetch* (outstanding
balance, monthly interest rate, outstanding fees) are now **passed in on the
command**, so allocation needs no loan/account repositories.

### Domain model (`domain/`)

`Payment` is a rich aggregate (not anemic): `allocate(...)` splits an amount
across fees → interest → principal, `lateFeeCharge(...)` is a factory that
encapsulates the fee schedule (flat $25 for ≤30 days; otherwise 5% of balance
capped at $50), and `settleAch()` performs the ACH settlement transition.
`PaymentAllocation` is an immutable value type for the split.

### Domain events (`event/`)

A base `DomainEvent` carries the envelope (`eventId`, `occurredAt`, `eventType`,
`version`). Concrete immutable events: `PaymentReceived`, `PaymentAllocated`,
`PaymentProcessed`, `LateFeesAssessed`. `EventPublisher` is the transport
abstraction; `SpringEventPublisher` backs it with `ApplicationEventPublisher`.
`PaymentEventLogger` is an `@EventListener` consumer demonstrating that
downstream reactions are decoupled (it can be swapped for a broker later).

### CQRS

- **Command side** (`command/`): `SubmitPaymentCommand`, `AssessLateFeeCommand`,
  `ProcessBatchCommand` request objects and `PaymentCommandService`, which
  mutates state and publishes events. No cross-domain repository access.
- **Query side** (`query/`): `PaymentQueryService` returns `PaymentView` read
  models for history/pending reads; it never mutates state or publishes events.

### Web (`web/`)

`PaymentController` mirrors the monolith's `/api/payments` surface but delegates
to the command/query services. `ApiExceptionHandler` (`@RestControllerAdvice`)
turns bean-validation failures into structured 400 responses.

## Tests

`@SpringBootTest` integration tests (MockMvc for the web layer) verify:

1. `POST /api/payments` returns 201 with a confirmation number and publishes
   `PaymentReceived` + `PaymentAllocated`.
2. An ACH payment additionally publishes `PaymentProcessed`.
3. Allocation splits an amount correctly across fees/interest/principal.
4. Assessing a late fee publishes `LateFeesAssessed` with the scheduled amount.
5. History/pending queries return the expected rows.

Events are captured with a test `@EventListener` (`RecordingEventListener`).
