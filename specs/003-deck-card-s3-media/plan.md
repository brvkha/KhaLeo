# Implementation Plan: Deck/Card Management and Media Uploads

**Branch**: `003-deck-card-s3-media` | **Date**: 2026-03-16 | **Spec**: `specs/003-deck-card-s3-media/spec.md`
**Input**: Feature specification from `/specs/003-deck-card-s3-media/spec.md`

**Note**: This template is filled in by the `/speckit.plan` command. See `.specify/templates/plan-template.md` for the execution workflow.

## Summary

Deliver end-to-end deck/card management and media-upload authorization for the
Spring Boot backend by extending existing relational deck/card services,
adding REST endpoints for deck/card CRUD and deck-scoped search with
pagination, and introducing AWS S3 presigned upload authorization with strict
validation (type, size, expiry) plus per-user throttling. The approach reuses
existing Aurora entities and observability conventions, enforces owner/admin
authorization for mutations, preserves public/private read rules, and keeps all
infrastructure changes Terraform-governed.

## Technical Context

**Language/Version**: Java 17 (Spring Boot 3.3.x), SQL (MySQL 8 via Flyway), Terraform HCL  
**Primary Dependencies**: Spring Web, Spring Security, Spring Data JPA/Hibernate, Flyway, AWS SDK v2 (`s3`, `s3-presigner`), JUnit 5, Testcontainers  
**Storage**: Aurora MySQL for decks/cards/media references and authorization records; DynamoDB unchanged for study activity logs  
**Testing**: JUnit 5 unit tests, Spring Boot integration tests with MySQL Testcontainers, MockMvc contract tests  
**Target Platform**: Dockerized Spring Boot backend on EC2 private subnets behind ALB/WAF; Terraform-managed AWS environment
**Project Type**: Web application (replicated monolith with backend + frontend + IaC)  
**Performance Goals**: Card search first-page response <= 2s for decks up to 10k cards; media authorization request handling <= 500ms p95 at expected load  
**Constraints**: Presigned upload URL expires in 5 minutes; allowed media types `image/jpeg`, `image/png`, `audio/mpeg`, `audio/webm`; max media size 5MB; 30 media-authorization requests per user per minute; list APIs must be paginated  
**Scale/Scope**: Feature scope is backend deck/card/media APIs, service/repository updates, observability updates, and optional Terraform alarm extensions for portfolio load (50 users, up to 30 concurrent)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

Initial Gate Assessment (Pre-Phase 0)

- Architecture gate: PASS. Design stays in the existing Java 17 Spring Boot
  monolith and Aurora/Dynamo split, with no stack deviations.
- Infrastructure gate: PASS. Potential alarm or config changes remain under
  `infra/terraform` as the only infrastructure source of truth.
- Security gate: PASS. Presigned S3 flow (no backend multipart proxy), media
  type/size validation, owner/admin mutation checks, and request throttling are
  explicit and aligned with constitutional controls.
- API gate: PASS. REST endpoints remain under `/api/v1/*`; all list endpoints
  in scope are defined as paginated.
- Observability gate: PASS. Plan includes structured JSON logging and metrics
  for success/failure/denial/rate-limit paths with CloudWatch/New Relic impact.
- Data gate: PASS. Aurora remains authoritative for deck/card/media-reference
  and optional authorization state; DynamoDB responsibilities remain unchanged.
- Quality gate: PASS. Testing strategy is mapped to unit/integration/contract
  layers consistent with the 80% and 60/30/10 quality policy.
- Compliance gate: PASS. No changes to SM-2 scheduling rules, card-state model,
  or account-level daily learning limit behavior.

Post-Design Re-check (Post-Phase 1)

- Architecture gate: PASS. `research.md`, `data-model.md`, and contracts keep
  implementation in existing backend modules and approved dependencies.
- Infrastructure gate: PASS. `quickstart.md` and contracts keep infra impacts
  additive and Terraform-governed.
- Security gate: PASS. Design artifacts encode public/private read behavior,
  owner/admin mutation authorization, strict media validation, 5-minute URL TTL,
  and per-user rate limits.
- API gate: PASS. Contract artifacts define paginated deck list and deck-card
  search contracts with explicit parameters and deterministic errors.
- Observability gate: PASS. Artifact set defines event/metric coverage for deck
  and card mutations, media authorization issuance/rejection, and throttling.
- Data gate: PASS. Aurora schema impacts and deletion/reference behavior are
  specified; DynamoDB remains unaffected.
- Quality gate: PASS. Phase artifacts define incremental automated tests across
  unit, integration, and contract layers for all critical behaviors.
- Compliance gate: PASS. No algorithm fidelity regressions; learning-state and
  daily-limit semantics remain unchanged.

## Project Structure

### Documentation (this feature)

```text
specs/003-deck-card-s3-media/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── deck-card-media-contract.md
└── tasks.md             # Created later by /speckit.tasks
```

### Source Code (repository root)

```text
backend/
├── src/main/java/com/khaleo/flashcard/
│   ├── config/
│   │   ├── auth/
│   │   ├── observability/
│   │   └── security/
│   ├── controller/
│   │   └── auth/
│   ├── entity/
│   ├── repository/
│   │   └── dynamo/
│   └── service/
│       ├── auth/
│       ├── activitylog/
│       └── persistence/
├── src/main/resources/
│   ├── application.yml
│   └── db/migration/
└── src/test/java/com/khaleo/flashcard/
  ├── contract/
  ├── integration/
  │   └── support/
  └── unit/

frontend/
└── src/

infra/
└── terraform/
  ├── main.tf
  ├── variables.tf
  ├── cloudwatch-auth-security-alarms.tf
  ├── cloudwatch-persistence-alarms.tf
  └── dynamodb-study-activity.tf
```

**Structure Decision**: Use the existing web-application monolith structure and
extend `backend/src/main/java/com/khaleo/flashcard/{controller,service,repository}`
for feature behavior; keep infra adjustments in `infra/terraform` and retain
frontend boundaries untouched for this phase.

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

No constitutional violations requiring justification.
