# Payment Processing Service

The **Payment Processing** bounded context extracted from the auto-finance monolith into a
standalone, event-driven Spring Boot microservice.

## Why this exists

In the monolith, `PaymentService` was synchronously coupled to the loan and account domains:
`submitPayment()` validated the loan via `LoanRepository`, read balances from
`AccountRepository`, mutated the account balance, flipped the loan status, and even called
`AccountService.closeAccount()` — all inline, in one transaction. A single payment touched
three domains' tables.

This service breaks that coupling:

- **Owns its data.** Its own `paymentdb` (H2) — no shared schema.
- **Publishes domain events instead of calling other domains.** The loan/account side
  effects become events that downstream services consume on their own schedule.
- **Rich domain model.** The `Payment` aggregate encapsulates its state transitions
  (`allocate`, `beginProcessing`, `complete`, `fail`) instead of being an anemic row.
- **CQRS.** Writes go through `PaymentCommandService`; reads go through
  `PaymentQueryService` and return immutable `PaymentView` projections.

## Domain events

| Event | When | Consumers |
|---|---|---|
| `PaymentReceived` | Borrower submits a payment | account-servicing, reporting |
| `PaymentAllocated` | Payment split into principal/interest/fees | account-servicing |
| `PaymentProcessed` | ACH/EFT settles (success/failure) | account-servicing, loan-origination, reporting |
| `LateFeesAssessed` | Late fee charged | account-servicing, reporting |

Events are published through the `EventPublisher` abstraction. The default
`SpringEventPublisher` uses Spring's in-process bus; swapping it for Kafka/SNS is the only
change needed to go cross-service. JSON Schemas live in [`../../events/payment/`](../../events/payment).

## API

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/payments` | Submit a payment (command) |
| `POST` | `/api/payments/late-fees` | Assess a late fee (command) |
| `GET` | `/api/payments/loan/{loanId}` | Payment history for a loan (query) |
| `GET` | `/api/payments/pending` | Pending payments (query) |

Allocation context (`outstandingBalance`, `annualInterestRate`, `outstandingFees`) is passed
in at the boundary — the service never reads the loan or account databases.

## Run

```bash
cd microservices/payment-service
mvn spring-boot:run     # http://localhost:8081
mvn test                # 10 integration tests
```

### Example

```bash
curl -X POST http://localhost:8081/api/payments \
  -H 'Content-Type: application/json' \
  -d '{"loanId":100,"paymentAmount":500.00,"paymentMethod":"ACH",
       "achRoutingNumber":"123456789","outstandingBalance":12000.00,
       "annualInterestRate":6.00,"outstandingFees":25.00}'
# => 201 Created; fees 25.00, interest 60.00, principal 415.00; status COMPLETED
```
