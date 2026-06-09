# Payment Service

A standalone, event-driven Spring Boot microservice extracted from the auto-finance
monolith's **Payment Processing** bounded context.

- **Spring Boot:** 2.7.18
- **Java:** 8
- **Package root:** `com.acme.payment` (no dependency on `com.acme.autofinance.*`)
- **Database:** its own in-memory H2 schema (`jdbc:h2:mem:paymentdb`) — no shared schema
- **Build:** its own `pom.xml`; it is **not** a module of the root monolith pom

## Why it exists

In the monolith, `PaymentService` was tightly, synchronously coupled to the loan and
account domains. Processing a payment directly:

- read the loan via `loanRepository` and the account via `accountRepository`,
- mutated the account balance (`updateAccountBalance`),
- marked the loan `PAID_OFF` and called `accountService.closeAccount(...)`
  (`updateLoanAfterPayment`).

This service breaks that coupling. The business logic is preserved, but every
cross-domain side effect becomes a **published domain event** that other services
subscribe to.

## Event-driven design

The service publishes four immutable domain events (all extend a common `DomainEvent`
base carrying `eventId`, `occurredAt`, `aggregateId`):

| Event | Replaces (in the monolith) |
|-------|----------------------------|
| `PaymentReceived` | — (new audit fact when a payment is accepted) |
| `PaymentAllocated` | inline fee/interest/principal split |
| `PaymentProcessed` | `updateAccountBalance` + `updateLoanAfterPayment` + `closeAccount` |
| `LateFeesAssessed` | inline fee row creation |

Publishing goes through an `EventPublisher` interface (so the transport can change
later). The default `SpringEventPublisher` is backed by Spring's
`ApplicationEventPublisher`. A `LoggingEventConsumer` records every published event —
standing in for the loan/account domains that would subscribe in a real system.

```
SubmitPaymentCommand
        │
        ▼
PaymentCommandService ──publish──> PaymentReceived
        │                          PaymentAllocated
        │ (ACH) ──publish────────> PaymentProcessed   ──► [account domain: reduce balance]
        │                                                  [loan domain: payoff / close account]
        ▼
   PaymentRepository (own H2)
```

JSON Schemas and example payloads for these events live in
[`events/payment/`](../../events/payment/) and [`events/README.md`](../../events/README.md).

## CQRS

The write and read sides are separated:

- **Command side** — `PaymentCommandService` handles `SubmitPaymentCommand`,
  `ProcessBatchCommand`, and `AssessLateFeeCommand`. The cross-domain context the
  monolith used to fetch synchronously (outstanding balance from the account domain,
  interest rate from the loan domain) is now provided as **command input**, so the
  service never reaches into other domains' repositories.
- **Query side** — `PaymentQueryService` returns immutable `PaymentView` projections
  rather than JPA entities.

## Preserved business logic

- **Allocation order:** late fees → interest → principal (`PaymentAllocator`).
- **ACH validation:** routing number must be exactly 9 digits, else the payment is
  marked `FAILED`.
- **Late-fee schedule:** flat `$25` for ≤ 30 days past due; otherwise `5%` of the
  outstanding balance, capped at `$50`.

## REST API

Mirrors the monolith's payment endpoints (`/api/payments`):

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/payments` | Submit a payment |
| `GET`  | `/api/payments/loan/{loanId}` | Payment history for a loan |
| `POST` | `/api/payments/batch` | Process a batch of payments |
| `GET`  | `/api/payments/pending` | List pending payments |
| `POST` | `/api/payments/late-fee` | Assess a late fee |

A `GlobalExceptionHandler` maps validation/not-found/unexpected errors to JSON responses.

## Build & test

This service builds and tests entirely on its own:

```bash
cd microservices/payment-service
mvn -B test     # runs unit + integration tests
mvn spring-boot:run   # starts on http://localhost:8081
```

### Tests

- `PaymentAllocationTest` — unit tests for the allocation split (fees → interest → principal).
- `PaymentEventFlowIntegrationTest` — `@SpringBootTest` tests that capture published
  events with a recording `@EventListener` and assert the ACH happy path
  (`PaymentReceived` → `PaymentAllocated` → `PaymentProcessed`) and the late-fee path
  (`LateFeesAssessed`).
