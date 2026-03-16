# Implementation Plan: SM-2 Spaced Repetition and Study Activity Logging

**Branch**: `004-sm2-study-logging` | **Date**: 2026-03-16 | **Spec**: `specs/004-sm2-study-logging/spec.md`
**Input**: Feature specification from `/specs/004-sm2-study-logging/spec.md`

**Note**: This template is filled in by the `/speckit.plan` command. See `.specify/templates/plan-template.md` for the execution workflow.

## Summary

Deliver study-session core behavior by adding due-card retrieval and card-rating
APIs that enforce account-level daily learning limits and SM-2 scheduling
fidelity (`NEW` -> `LEARNING` -> `MASTERED` waiting -> `REVIEW`), then emit
asynchronous study activity logs to DynamoDB without blocking user-facing API
responses. The design keeps scheduling state authoritative in Aurora MySQL,
records immutable activity events in DynamoDB, and adds observability for
rating, retrieval, and async logging outcomes.

## Technical Context

<!--
  ACTION REQUIRED: Replace the content in this section with the technical details
  for the project. The structure here is presented in advisory capacity to guide
  the iteration process.
-->

**Language/Version**: Java 17 (Spring Boot 3.3.x), SQL (Aurora MySQL 8 via Flyway), Terraform HCL  
**Primary Dependencies**: Spring Web, Spring Security, Spring Data JPA/Hibernate, Flyway, AWS SDK v2 DynamoDB Enhanced Client, Spring Async, JUnit 5, Testcontainers  
**Storage**: Aurora MySQL for card-learning state and quota calculations; DynamoDB (`StudyActivityLog`) for append-only study activity entries  
**Testing**: JUnit 5 unit tests, Spring integration tests (Testcontainers MySQL), MockMvc contract tests  
**Target Platform**: Dockerized Spring Boot backend on AWS EC2 private subnets behind ALB/WAF; Terraform-managed infra
**Project Type**: Web application (replicated monolith with backend + frontend + IaC)  
**Performance Goals**: 95% of rating requests return in <1s; 99% of successful ratings persist activity log entries within 30s; due-card fetch p95 <750ms at portfolio load  
**Constraints**: SM-2 math fidelity with minimum ease factor 1.3; account-level daily new-card limits; async DynamoDB writes must not block rating response path; REST + pagination for list endpoint  
**Scale/Scope**: Backend study endpoints, scheduling service, async activity logging service, observability updates, and any additive Terraform alarm updates for expected load (~50 users, ~30 concurrent)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

Initial Gate Assessment (Pre-Phase 0)

- Architecture gate: PASS. Feature remains in existing Java 17 Spring Boot
  monolith and approved Aurora/DynamoDB storage split.
- Infrastructure gate: PASS. Any DynamoDB/CloudWatch adjustments remain in
  `infra/terraform`; no manual console-only changes are assumed.
- Security gate: PASS. Existing JWT/auth boundaries are preserved and study
  actions require authenticated/authorized access to user-owned deck content.
- API gate: PASS. Endpoints are RESTful and the list-producing next-cards API
  defines paginated/bounded retrieval semantics.
- Observability gate: PASS. Plan includes structured telemetry for due-card
  fetch outcomes, rating transitions, async log success/failure, and alarm impact.
- Data gate: PASS. Aurora is authoritative for mutable scheduling state; DynamoDB
  receives immutable activity events; Flyway-only relational changes remain mandated.
- Quality gate: PASS. Test strategy spans unit, integration, and contract layers
  to stay aligned with 80% and 60/30/10 policy targets.
- Compliance gate: PASS. Feature directly implements constitution-critical SM-2,
  card-state transitions, and account-level daily learning limits.

Post-Design Re-check (Post-Phase 1)

- Architecture gate: PASS. `research.md`, `data-model.md`, and contracts keep
  implementation within approved backend modules and dependencies.
- Infrastructure gate: PASS. `quickstart.md` and contracts keep infra impact
  additive and Terraform-governed for DynamoDB/alarm configuration.
- Security gate: PASS. Contracts enforce authenticated study operations and
  consistent denial behavior for inaccessible decks/cards.
- API gate: PASS. API contract defines the paginated next-cards response and
  deterministic rating request/response semantics.
- Observability gate: PASS. Artifact set defines required logs/metrics for
  retrieval, rating, and async log-write outcomes.
- Data gate: PASS. Data model cleanly separates mutable relational scheduling
  state from immutable DynamoDB activity records.
- Quality gate: PASS. Design artifacts define layered automated tests for SM-2
  transition correctness, quota behavior, and async logging resilience.
- Compliance gate: PASS. Post-design artifacts preserve SM-2 fidelity, required
  card states, and account-level daily learning-limit enforcement.

## Project Structure

### Documentation (this feature)

```text
specs/004-sm2-study-logging/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── study-engine-contract.md
└── tasks.md             # Created later by /speckit.tasks
```

### Source Code (repository root)
<!--
  ACTION REQUIRED: Replace the placeholder tree below with the concrete layout
  for this feature. Delete unused options and expand the chosen structure with
  real paths (e.g., apps/admin, packages/something). The delivered plan must
  not include Option labels.
-->

```text
backend/
├── src/main/java/com/khaleo/flashcard/
│   ├── controller/
│   │   └── study/
│   ├── service/
│   │   ├── study/
│   │   ├── activitylog/
│   │   └── persistence/
│   ├── repository/
│   │   └── dynamo/
│   ├── entity/
│   └── config/
├── src/main/resources/
│   └── db/migration/
└── src/test/java/com/khaleo/flashcard/
  ├── contract/
  ├── integration/
  └── unit/

infra/
└── terraform/
  ├── dynamodb-study-activity.tf
  ├── cloudwatch-persistence-alarms.tf
  └── cloudwatch-auth-security-alarms.tf

KhaLeoDocs/
└── sm2_engine_and_dynamodb_logging.md
```

**Structure Decision**: Keep the existing web-application monolith structure,
implementing study scheduling and activity logging in `backend/` and capturing
any DynamoDB/monitoring infra impact only under `infra/terraform`.

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

No constitutional violations requiring justification.
