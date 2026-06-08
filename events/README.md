# Domain Event Catalog

Schemas for the domain events that replace synchronous cross-domain calls in the legacy
monolith. The full analysis-generated catalog (14 proposed events across 5 contexts) lives
in [`dashboard/data/event_catalog.json`](../dashboard/data/event_catalog.json).

This directory holds the **implemented** contracts for the first extracted bounded context,
**Payment Processing**. Each event is an immutable, past-tense record published by
`microservices/payment-service` and consumed asynchronously by other contexts.

## Payment Processing events

| Event | Trigger | Replaces (monolith) | Consumers |
|---|---|---|---|
| `payment.received` | Borrower submits a payment | inline body of `PaymentService.submitPayment()` | account-servicing, reporting |
| `payment.allocated` | Payment split into principal/interest/fees | `PaymentService.allocatePayment()` mutation | account-servicing |
| `payment.processed` | ACH/EFT payment settles (success/failure) | `PaymentService.processAchPayment()` synchronous account + loan updates | account-servicing, loan-origination, reporting |
| `payment.late_fees_assessed` | Late fee charged to a delinquent account | `PaymentService.assessLateFee()` called from `LoanService.runEndOfDayProcessing()` | account-servicing, reporting |

JSON Schemas for each payload are in [`payment/`](payment/).
