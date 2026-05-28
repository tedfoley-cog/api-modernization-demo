# Implementation Plan — API Modernization Demo

## What the Demo Proves

A legacy monolithic auto-finance REST API — tightly coupled controllers, shared database, synchronous service calls, anemic domain model — can be analyzed, decomposed, and partially modernized into event-driven microservices by an AI agent in a single live session. The audience sees the coupling quantified, domain boundaries identified, and a working event-driven microservice produced with domain events, CQRS separation, and integration tests.

## What Devin Does Live

Devin analyzes the monolith, runs Python analysis scripts to map endpoints/dependencies/coupling, populates an interactive modernization dashboard, then converts one bounded context (Payment Processing) into an event-driven Spring Boot microservice with domain events and integration tests.

## Stack and Rationale

| Component | Choice | Source / Rationale |
|---|---|---|
| Legacy runtime | Java 8 source level, Spring Boot 2.7.18 | Matches real-world legacy stack; same pattern as `java-migration-demo` |
| Build tool | Maven 3.x | Standard for enterprise Java; `pom.xml` with `spring-boot-starter-parent` |
| Database | H2 in-memory | Zero external deps; Spring Boot auto-config; ref: [Spring Boot H2 docs](https://docs.spring.io/spring-boot/docs/2.7.x/reference/html/data.html#data.sql.h2-web-console) |
| REST framework | Spring MVC `@RestController` | Standard synchronous REST; ref: [Spring MVC docs](https://docs.spring.io/spring-framework/docs/5.3.x/reference/html/web.html#mvc) |
| ORM | Spring Data JPA + Hibernate | Standard for entity mapping; ref: [Spring Data JPA docs](https://docs.spring.io/spring-data/jpa/docs/2.7.x/reference/html/) |
| API docs | springdoc-openapi 1.8.0 | Swagger UI at `/swagger-ui.html`; ref: [springdoc docs](https://springdoc.org/) |
| Analysis scripts | Python 3.11+ | Parse Java AST via regex (no external Java parser needed); produce JSON |
| Dashboard | Vanilla HTML/JS + Chart.js | Zero build step; opens in browser; Chart.js via CDN |
| Target architecture | Spring Boot + in-memory event bus | Simulates Kafka without requiring broker infra |

## Repo Layout

```
api-modernization-demo/
├── docs/
│   ├── IMPLEMENTATION_PLAN.md        ← This file
│   ├── flowchart.html                ← Interactive demo flow (Mermaid)
│   └── flowchart.png                 ← Flowchart image for README
├── src/main/java/com/acme/autofinance/
│   ├── AutoFinanceApplication.java   ← Spring Boot main class
│   ├── config/
│   │   └── AppConfig.java            ← Shared configuration
│   ├── controller/
│   │   ├── LoanController.java       ← /api/loans endpoints
│   │   ├── PaymentController.java    ← /api/payments endpoints
│   │   ├── AccountController.java    ← /api/accounts endpoints
│   │   ├── DealerController.java     ← /api/dealers endpoints
│   │   └── ReportController.java     ← /api/reports endpoints
│   ├── service/
│   │   ├── LoanService.java          ← God service — origination + servicing + reporting (~500 lines)
│   │   ├── PaymentService.java       ← Payment processing with synchronous coupling
│   │   ├── AccountService.java       ← Account servicing with cross-domain queries
│   │   ├── DealerService.java        ← Dealer integration, tightly coupled to loans
│   │   └── ReportService.java        ← Direct SQL queries against shared tables
│   ├── model/
│   │   ├── LoanApplication.java      ← Loan entity (anemic — just getters/setters)
│   │   ├── Payment.java              ← Payment entity
│   │   ├── Account.java              ← Account entity
│   │   ├── Dealer.java               ← Dealer entity
│   │   ├── DealPackage.java          ← Deal submission entity
│   │   └── CreditDecision.java       ← Credit check result
│   ├── repository/
│   │   ├── LoanRepository.java       ← JPA repository
│   │   └── PaymentRepository.java    ← JPA repository
│   └── ...
├── src/main/resources/
│   ├── application.properties        ← Monolithic config
│   └── data.sql                      ← Seed data
├── analysis/
│   ├── analyze_monolith.py           ← Main analysis script
│   ├── dependency_graph.py           ← Service dependency graph builder
│   ├── coupling_scorer.py            ← Coupling score calculator
│   ├── domain_boundary.py            ← Domain boundary identifier
│   ├── event_catalog.py              ← Event catalog generator
│   └── requirements.txt              ← Python dependencies (none — stdlib only)
├── dashboard/
│   ├── index.html                    ← Interactive modernization dashboard
│   └── data/                         ← JSON output from analysis (starts empty)
├── microservices/                    ← Empty — Devin fills during live demo
├── events/                           ← Empty — Devin fills during live demo
├── .github/workflows/
│   └── ci.yml                        ← Simple CI: compile Java + lint Python
├── pom.xml                           ← Maven build
├── README.md                         ← Project documentation
└── DEMO_NOTES.md                     ← Presenter cheat sheet
```

## Flowchart Outline

Nodes and edges for the demo-flow diagram:

1. **MONOLITH** → Legacy API Monolith
2. **PROMPT** → Prompt Devin
3. **ANALYZE** subgraph (Devin Analysis):
   - MAP → Map API Endpoints
   - DEPS → Trace Dependencies
   - SCORE → Score Coupling
   - BOUNDS → Identify Boundaries
4. **DASHBOARD** → Generate Dashboard
5. **CONVERT** subgraph (Devin Modernization):
   - EXTRACT → Extract Bounded Context
   - EVENTS → Define Domain Events
   - SERVICE → Build Microservice
   - TESTS → Add Integration Tests
6. **REVIEW** → Presenter Opens Dashboard
7. **COMPARE** → Before/After Comparison

## Runtime Plan

1. `mvn spring-boot:run` — starts the legacy monolith on port 8080 with H2
2. Swagger UI at `http://localhost:8080/swagger-ui.html` shows all endpoints
3. Analysis: `python analysis/analyze_monolith.py` — parses Java source, outputs JSON to `dashboard/data/`
4. Dashboard: open `dashboard/index.html` in browser — shows analysis results
5. Devin converts Payment bounded context → writes to `microservices/payment-service/`

## CI Plan

Single workflow `.github/workflows/ci.yml`:
- Checkout repo
- Set up Java 17, compile with `mvn -B compile`
- Set up Python 3.11, run `ruff check analysis/`
- ~30 lines of YAML

## Risks and Unknowns

1. Spring Boot 2.7.18 uses Java 8 source compatibility but compiles fine on Java 17 — confirmed via `java-migration-demo` pattern.
2. Analysis scripts use regex-based Java parsing (not a full AST parser) — sufficient for demo quality but won't handle every edge case. This is acceptable since the scripts only need to work against our own source files.
3. The event bus in the converted microservice is in-memory (not Kafka) — called out in README as "simulating Kafka for demo portability."
