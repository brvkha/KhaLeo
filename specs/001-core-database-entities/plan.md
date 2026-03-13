# Implementation Plan: Core Database and Entity Foundation

**Branch**: `001-core-database-entities` | **Date**: 2026-03-13 | **Spec**: `specs/001-core-database-entities/spec.md`
**Input**: Feature specification from `/specs/001-core-database-entities/spec.md`

## Summary

Deliver the foundational persistence layer for Kha Leo Flashcard by defining
Aurora MySQL schema evolution with Flyway, Java 17 Spring Boot JPA entities for
User/Deck/Card/CardLearningState, and DynamoDB activity-log modeling. The plan
enforces source-of-truth relational commits, asynchronous activity logging,
single active learning state per `(userId, cardId)`, optimistic concurrency,
and constitutional observability/testing/security gates.

## Technical Context

**Language/Version**: Java 17 (backend), SQL (MySQL 8.0 dialect), Terraform HCL  
**Primary Dependencies**: Spring Boot, Spring Data JPA, Hibernate, Flyway,
Lombok, AWS SDK v2 (DynamoDB), Testcontainers, JUnit 5  
**Storage**: AWS Aurora MySQL (core transactional data), AWS DynamoDB
(`StudyActivityLog` immutable events)  
**Testing**: JUnit 5 + Spring Boot Test + Testcontainers (integration), contract
validation for persistence constraints, targeted concurrency tests  
**Target Platform**: Dockerized Spring Boot service on AWS EC2 private subnets
behind ALB/WAF; Terraform-managed AWS resources  
**Project Type**: Web application (replicated monolith, backend + frontend + IaC)  
**Performance Goals**: Sustain expected workload of 50 total users and 30
concurrent users without data-integrity violations; activity logs queryable by
user within 5 seconds for at least 95% sampled events  
**Constraints**: No `ddl-auto` schema drift in production; Flyway-only schema
changes; exactly one active learning-state row per `(userId, cardId)`; one
bounded retry for optimistic-lock conflict; card sides must each contain text or
media; daily learning limit range 1..9999 with default 9999  
**Scale/Scope**: Initial portfolio scope covering 5 core domain models,
foundational persistence behavior, and reliability patterns for dual-store
writes

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

Initial Gate Assessment (Pre-Phase 0)

- Architecture gate: PASS. Scope remains within mandated replicated monolith
  stack and storage split.
- Infrastructure gate: PASS. Feature requires Terraform-managed Aurora and
  DynamoDB definitions with no manual resource drift.
- Security gate: PASS. No auth-token lifetime changes; user identity and
  integrity constraints are preserved; no media-upload flow changes.
- API gate: PASS. No list endpoint is introduced in this feature; pagination is
  explicitly marked not applicable in the spec.
- Observability gate: PASS. Research/design includes failure telemetry for
  relational writes, async log retries, dead-letter events, and conflict paths.
- Data gate: PASS. Flyway migration path and dual-store responsibility are
  defined.
- Quality gate: PASS. Plan includes unit/integration/concurrency validation work
  aligned with 80% and 60/30/10 targets.
- Compliance gate: PASS. SM-2 state persistence, account learning limits, and
  card-state semantics are directly addressed.

Post-Design Re-check (Post-Phase 1)

- Architecture gate: PASS. `data-model.md` and `research.md` keep mandated
  backend stack and storage boundaries.
- Infrastructure gate: PASS. `quickstart.md` and `contracts/persistence-contract.md`
  require Terraform-managed table/schema definitions only.
- Security gate: PASS. Entity constraints enforce identity uniqueness,
  deterministic validation, and auditable timestamps.
- API gate: PASS. No public API contract needed; persistence contract is scoped
  to data integrity and schema compatibility.
- Observability gate: PASS. Asynchronous logging, retry/dead-letter, and failure
  visibility are documented as acceptance requirements.
- Data gate: PASS. Relational source-of-truth plus async event log design is
  formalized.
- Quality gate: PASS. Test strategy includes rejection cases, migration
  validation, and optimistic-concurrency conflict tests.
- Compliance gate: PASS. SM-2-supporting fields and constrained state model are
  fully represented.

## Project Structure

### Documentation (this feature)

```text
specs/001-core-database-entities/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── persistence-contract.md
└── tasks.md             # Created later by /speckit.tasks
```

### Source Code (repository root)

```text
backend/
├── src/main/java/com/khaleo/flashcard/
│   ├── entity/
│   ├── model/dynamo/
│   ├── repository/
│   └── config/
├── src/main/resources/
│   └── db/migration/
└── src/test/java/com/khaleo/flashcard/
    ├── unit/
    ├── integration/
    └── contract/

frontend/
└── src/

infra/
└── terraform/
```

**Structure Decision**: Use the web-application structure with dedicated
`backend/`, `frontend/`, and `infra/terraform/` directories. The current
repository is documentation-first; these source paths are the target scaffold
for implementation tasks.

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

No constitutional violations requiring justification.
