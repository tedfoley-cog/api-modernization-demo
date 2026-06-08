# Domain Events Catalog

This directory holds the **contract-first event specifications** for the
event-driven modernization of the auto-finance monolith. It is a
language-agnostic design artifact: the schemas and tables below define the
contracts that producing and consuming services agree on, independent of the
Java implementation.

## Why events?

Today the monolith couples its five domains through **synchronous in-process
method calls** that share a single database. For example,
`PaymentService.submitPayment()` calls `allocatePayment()` inline and directly
mutates account state, and `LoanService.runEndOfDayProcessing()` reaches across
into the payment domain to assess late fees. These cross-domain calls make the
domains impossible to deploy, scale, or reason about independently.

The modernization replaces those synchronous calls with **domain events**
published on an **event bus** using a **publish/subscribe** pattern:

- A **producer** owns a business capability. When something meaningful happens
  (a payment is received, a loan is funded), it publishes an immutable event
  describing that fact.
- The event bus delivers the event to every interested **consumer**. Producers
  do not know or care who consumes the event.
- Consumers react asynchronously (update their own state, trigger workflows,
  feed reporting) without the producer holding a synchronous dependency on them.

This inverts the dependency direction: instead of `PaymentService` calling
`AccountService` directly, `PaymentService` publishes `PaymentProcessed` and the
account-servicing service subscribes to it.

## Event envelope

Every event shares a common envelope so the bus and consumers can route,
de-duplicate, and version messages consistently:

| Field        | Type              | Description                                          |
| ------------ | ----------------- | ---------------------------------------------------- |
| `eventId`    | string (uuid)     | Unique identifier for this event instance.           |
| `eventType`  | string (const)    | The event name, e.g. `PaymentReceived`.              |
| `occurredAt` | string (date-time)| When the business fact occurred (ISO 8601, UTC).     |
| `version`    | integer           | Schema version of the event contract.                |
| `data`       | object            | Event-specific payload (see per-event schemas).      |

JSON Schema (draft-07) definitions for the **Payment Processing** events live
under [`payment/`](./payment/), with example instances in
[`payment/examples.md`](./payment/examples.md).

## Full event catalog

All domain events identified across the monolith, grouped by the domain that
**produces** them. "Replaces" names the current synchronous implementation the
event supersedes.

### Loan Origination

| Event | Trigger | Producers | Consumers | Replaces (current synchronous call) |
| ----- | ------- | --------- | --------- | ----------------------------------- |
| `LoanApplicationSubmitted` | New loan application created | loan-origination-service | credit-decisioning, dealer-integration-service | Synchronous method call in `LoanService.createApplication()` |
| `CreditDecisionMade` | Credit check completed (approved or declined) | loan-origination-service | account-servicing-service, dealer-integration-service, reporting-service | Inline in `LoanService.performCreditCheck()` — synchronous, no event |
| `LoanFunded` | Loan moves to FUNDED status | loan-origination-service | account-servicing-service, dealer-integration-service, reporting-service | `LoanService.fundLoan()` synchronously updates account and deal package |

### Payment Processing

| Event | Trigger | Producers | Consumers | Replaces (current synchronous call) |
| ----- | ------- | --------- | --------- | ----------------------------------- |
| `PaymentReceived` | Payment submitted by borrower | payment-processing-service | account-servicing-service, reporting-service | `PaymentService.submitPayment()` — synchronous, calls allocatePayment inline |
| `PaymentAllocated` | Payment broken down into principal/interest/fees | payment-processing-service | account-servicing-service | Inline in `PaymentService.allocatePayment()` — synchronous |
| `PaymentProcessed` | ACH/EFT payment completed successfully | payment-processing-service | account-servicing-service, loan-origination-service, reporting-service | `PaymentService.processAchPayment()` synchronously updates account balance and loan status |
| `LateFeesAssessed` | Account past due, late fee charged | payment-processing-service | account-servicing-service, reporting-service | `PaymentService.assessLateFee()` — called synchronously from `LoanService.runEndOfDayProcessing()` |

### Account Servicing

| Event | Trigger | Producers | Consumers | Replaces (current synchronous call) |
| ----- | ------- | --------- | --------- | ----------------------------------- |
| `AccountCreated` | New account created after loan approval | account-servicing-service | reporting-service | Inline in `LoanService.createApplication()` — wrong service creates the account |
| `AccountBalanceUpdated` | Balance changes after payment, adjustment, or payoff | account-servicing-service | reporting-service, loan-origination-service | `PaymentService.updateAccountBalance()` — payment service directly mutates account state |
| `AccountDelinquent` | Account crosses delinquency threshold | account-servicing-service | payment-processing-service, reporting-service | `LoanService.runEndOfDayProcessing()` — loan service manages account delinquency |

### Dealer Integration

| Event | Trigger | Producers | Consumers | Replaces (current synchronous call) |
| ----- | ------- | --------- | --------- | ----------------------------------- |
| `DealPackageSubmitted` | Dealer submits a deal package | dealer-integration-service | loan-origination-service | `DealerService.submitDealPackage()` synchronously creates loan application |
| `DealerSettlementCalculated` | Settlement amounts computed for dealer | dealer-integration-service | reporting-service | `DealerService.getDealerSettlement()` — computed on-demand, not event-driven |

---

_Source of truth: `analysis/event_catalog.py`, materialized as
`event_catalog.json` by `python -m analysis.analyze_monolith`. Do not hand-edit
event definitions here — regenerate from the catalog._
