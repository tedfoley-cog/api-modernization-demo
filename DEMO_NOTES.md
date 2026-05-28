# Demo Cheat Sheet — API Modernization

## Setup (do this before joining the call)
- [ ] Open repo in a fresh Devin session
- [ ] Confirm monolith compiles: `mvn -B compile` should succeed

## Demo Flow

1. **Show the monolith** — open Swagger UI (`/swagger-ui.html`) to show 20+ endpoints across 5 tightly-coupled domains sharing one database. "This is what a typical API monolith looks like after 5-8 years of organic growth — every service talks to every other service, one change requires testing everything."

2. **Prompt Devin** — "Analyze this auto finance monolith. Map all endpoints, trace service dependencies, score coupling, identify domain boundaries, and generate the modernization dashboard. Then convert the Payment Processing bounded context into an event-driven microservice with domain events and integration tests."

3. **Devin runs analysis** — maps 20+ endpoints, traces cross-domain calls (LoanService alone touches 5 repositories across 4 domains), scores coupling, identifies 5 bounded contexts. Dashboard populates with endpoint inventory, dependency graph, coupling scores, and event catalog. "Devin maps the entire API surface and its internal coupling in minutes — this analysis alone would take a team weeks of manual code review."

4. **Walk through the dashboard** — show coupling scores (LoanService: highest risk), domain boundaries, and the 14 proposed domain events. "Event-driven architecture decouples these services — a payment no longer synchronously calls the loan service, it publishes a PaymentReceived event that any downstream service can consume."

5. **Devin converts Payment Processing** — extracts bounded context into standalone Spring Boot microservice with domain events (PaymentReceived, PaymentAllocated, PaymentProcessed, LateFeesAssessed), CQRS separation, and integration tests. Opens a PR. "The same Devin session that analyzed the monolith produces working microservice code with domain events, CQRS patterns, and integration tests."

6. **Close with ROI framing** — "This is how you accelerate EPI modernization — Devin handles the analysis AND the implementation, your team reviews and merges. Weeks of analysis become minutes. Months of implementation become days."
