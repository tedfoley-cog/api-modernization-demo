# Event Catalog

This directory holds the JSON Schemas and documentation for the domain events
published as part of the monolith modernization effort. Events are the contract
that lets bounded contexts (payment, loan, account, ...) communicate
asynchronously instead of calling into each other synchronously.

## Payment domain events

Published by the **payment-service** (`microservices/payment-service/`). These
events replace the monolith's synchronous cross-domain calls
(`PaymentService` directly invoking `loanRepository`, `accountRepository`, and
`accountService.closeAccount(...)`). Instead, the payment service publishes facts
and the loan/account domains subscribe and update their own state.

| Event | Schema | When it is published | Typical consumers |
|-------|--------|----------------------|-------------------|
| `payment.received` | [payment.received.schema.json](payment/payment.received.schema.json) | A payment is accepted (status `PENDING`), before allocation/processing | Audit, fraud, notifications |
| `payment.allocated` | [payment.allocated.schema.json](payment/payment.allocated.schema.json) | A payment amount is split into fees → interest → principal | Loan domain (amortization) |
| `payment.processed` | [payment.processed.schema.json](payment/payment.processed.schema.json) | A payment finishes processing (e.g. ACH settlement) | Account domain (balance), Loan domain (payoff/closure) |
| `payment.late_fees_assessed` | [payment.late_fees_assessed.schema.json](payment/payment.late_fees_assessed.schema.json) | A late fee is assessed against a loan | Account domain, notifications |

### Common envelope

Every event extends a common `DomainEvent` base and therefore carries:

| Field | Type | Description |
|-------|------|-------------|
| `eventId` | string (uuid) | Unique id for this event instance |
| `eventType` | string | Stable logical name (e.g. `payment.received`) |
| `occurredAt` | string (date-time) | UTC timestamp of when the event occurred |
| `aggregateId` | string | Aggregate the event pertains to (payment id, or loan id for late fees) |

## Example payloads

### `payment.received`
```json
{
  "eventId": "16950294-c99e-4872-9ae2-a20364e360fe",
  "eventType": "payment.received",
  "occurredAt": "2026-06-09T18:40:26.517Z",
  "aggregateId": "501",
  "paymentId": 501,
  "loanId": 1001,
  "paymentAmount": 500.00,
  "paymentMethod": "ACH",
  "confirmationNumber": "PMT-9F3A1B2C"
}
```

### `payment.allocated`
```json
{
  "eventId": "97325311-16a4-4fea-83bd-3dc7912b23f9",
  "eventType": "payment.allocated",
  "occurredAt": "2026-06-09T18:40:26.517Z",
  "aggregateId": "501",
  "paymentId": 501,
  "loanId": 1001,
  "feeAmount": 0.00,
  "interestAmount": 100.00,
  "principalAmount": 400.00
}
```

### `payment.processed`
```json
{
  "eventId": "1ce95cd4-964b-4512-8343-4054c0cd0738",
  "eventType": "payment.processed",
  "occurredAt": "2026-06-09T18:40:26.531Z",
  "aggregateId": "501",
  "paymentId": 501,
  "loanId": 1001,
  "status": "COMPLETED",
  "principalApplied": 400.00,
  "totalCompletedToDate": 500.00
}
```

### `payment.late_fees_assessed`
```json
{
  "eventId": "a5427c27-b717-4fa2-a52a-23db9079c7de",
  "eventType": "payment.late_fees_assessed",
  "occurredAt": "2026-06-09T18:40:26.544Z",
  "aggregateId": "2001",
  "paymentId": 777,
  "loanId": 2001,
  "lateFee": 25.00,
  "daysPastDue": 15
}
```

## Typical flows

- **ACH payment (happy path):** `payment.received` → `payment.allocated` → `payment.processed`
- **Late fee assessment:** `payment.late_fees_assessed`
