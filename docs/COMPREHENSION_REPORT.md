# Monolith Comprehension Report — Auto Finance API

> Generated from the static-analysis output in `dashboard/data/` (produced by
> `python -m analysis.analyze_monolith src dashboard/data`). All numbers below are
> sourced directly from those JSON artifacts.

## Overview

The `com.acme.autofinance` codebase is a tightly-coupled Spring Boot monolith spanning
five business domains that share a single H2 database.

| Metric | Value |
| --- | --- |
| Java files | 28 |
| Total lines of code | 2,237 |
| Service-layer lines of code | 1,167 |
| REST endpoints | 24 |
| Service classes | 5 |
| Avg. methods per service | 6.4 |
| JPA entities | 5 |
| `@Autowired` (cross-domain) injections | 20 |
| Dependency-graph nodes / edges | 28 / 33 |
| Proposed domain events | 12 |

## API Endpoints (24 total)

Endpoints are distributed across five controllers:

- **LoanController** — loan origination (create, fetch, list, update terms, fund, end-of-day)
- **PaymentController** — payment submission, batch processing, pending queue, per-loan history
- **AccountController** — account lookup, address update, payoff quote, early termination, delinquency
- **DealerController** — deal submission, settlement, inventory financing, dealer listing
- **ReportController** — portfolio, delinquency, and dealer reports

Several endpoints cross domain lines (e.g. `GET /api/payments/loan/{loanId}` reaches into
loan origination, and `GET /api/reports/dealers` reaches into dealer integration), which is
a symptom of the shared-database coupling.

## Service Coupling & Risk

| Service | Domain | LOC | Methods | Cross-domain calls | Risk score |
| --- | --- | ---: | ---: | ---: | ---: |
| **LoanService** | loan-origination | 478 | 11 | 7 | **30.3** |
| AccountService | account-servicing | 165 | 8 | 4 | 19.6 |
| PaymentService | payment-processing | 240 | 6 | 5 | 19.4 |
| DealerService | dealer-integration | 159 | 4 | 4 | 13.6 |
| ReportService | reporting | 125 | 3 | 0 | 4.8 |

**`LoanService` is the "god service."** At 478 LOC and 11 methods it is roughly 3× the size
of the median service, makes 7 cross-domain calls, and carries the highest risk score (30.3).
It also creates accounts, mutates account balances, and drives end-of-day processing for other
domains — responsibilities that belong elsewhere. `ReportService` is the cleanest (read-only,
0 cross-domain calls, risk 4.8).

The total cross-domain service-call count is **20** (sum of per-service cross-domain calls),
matching the 20 `@Autowired` cross-domain injections detected.

## Bounded Contexts (5) & Extraction Complexity

| Bounded context | Proposed service | Endpoints | Entities | Cross-boundary deps | Complexity |
| --- | --- | ---: | ---: | ---: | --- |
| loan-origination | `loan-origination-service` | 8 | 1 | 11 | **HIGH** |
| payment-processing | `payment-processing-service` | 4 | 1 | 8 | MEDIUM |
| account-servicing | `account-servicing-service` | 6 | 1 | 8 | MEDIUM |
| dealer-integration | `dealer-integration-service` | 5 | 2 | 7 | MEDIUM |
| reporting | `reporting-service` | 3 | 0 | 1 | LOW |

The JPA entities are `LoanApplication` (19 fields), `Payment` (15), `Account` (15),
`DealPackage` (13), and `Dealer` (11). Loan origination is the hardest to extract (11
cross-boundary dependencies); reporting is the easiest (read-only, 1 dependency).

## Proposed Domain Events (12)

Event-driven decomposition replaces synchronous cross-service method calls with these events:

| Event | Producer | Consumers |
| --- | --- | --- |
| LoanApplicationSubmitted | loan-origination-service | credit-decisioning, dealer-integration-service |
| CreditDecisionMade | loan-origination-service | account-servicing, dealer-integration, reporting |
| LoanFunded | loan-origination-service | account-servicing, dealer-integration, reporting |
| PaymentReceived | payment-processing-service | account-servicing, reporting |
| PaymentAllocated | payment-processing-service | account-servicing |
| PaymentProcessed | payment-processing-service | account-servicing, loan-origination, reporting |
| LateFeesAssessed | payment-processing-service | account-servicing, reporting |
| AccountCreated | account-servicing-service | reporting |
| AccountBalanceUpdated | account-servicing-service | reporting, loan-origination |
| AccountDelinquent | account-servicing-service | payment-processing, reporting |
| DealPackageSubmitted | dealer-integration-service | loan-origination |
| DealerSettlementCalculated | dealer-integration-service | reporting |

Each event today is an inline synchronous call (e.g. `PaymentService.processAchPayment()`
synchronously updates account balance and loan status). Converting these to asynchronous
events is what breaks the shared-database coupling.

## Modernization Status

The **Payment Processing** bounded context is the first to be extracted into an event-driven
microservice (`payment-processing-service`). It is a MEDIUM-complexity context with 4 endpoints
and a single owning entity (`Payment`), making it a sensible first cut.

**Progress: 1 / 5 bounded contexts converted (20%).**
