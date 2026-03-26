# Implementation Plan: Domain-Agnostic Rich Card Content and Study UI Layout

**Branch**: `009-domain-agnostic-rich-cards` | **Date**: 2026-03-26 | **Spec**: `specs/009-domain-agnostic-rich-cards/spec.md`
**Input**: Feature specification from `/specs/009-domain-agnostic-rich-cards/spec.md`

**Note**: This template is filled in by the `/speckit.plan` command. See `.specify/templates/plan-template.md` for the execution workflow.

## Summary

Extend card authoring and study rendering from front/back-only to a domain-agnostic rich-card payload (`term`, `answer`, optional media/metadata, examples array) while preserving existing FSRS v6 scheduling behavior. Implementation uses additive Flyway migration, strict request validation (HTTPS allowlist URLs, bounded examples, payload limit), full-object replace update semantics, optimistic concurrency (`409` on stale writes), and a study layout that keeps core content visible with examples-only scrolling and mobile-safe fixed rating controls.

## Technical Context

**Language/Version**: Java 17 (Spring Boot backend), TypeScript (React frontend), Terraform HCL  
**Primary Dependencies**: Spring Web/Security/Data JPA, Hibernate, Flyway, React, Tailwind CSS, Vitest, Playwright  
**Storage**: Aurora MySQL for card/deck domain data and migration; DynamoDB unchanged for activity logging  
**Testing**: JUnit 5 + Spring integration/contract tests; Vitest + React Testing Library + Playwright E2E  
**Target Platform**: AWS-hosted web app (CloudFront/S3 frontend + EC2 backend behind ALB/WAF)
**Project Type**: Web application (replicated monolith)  
**Performance Goals**: Preserve existing study rating latency targets; create/update validation must remain interactive; no regression in FSRS v6 response path  
**Constraints**: No algorithmic scheduling change; plain-text rendering policy; max 20 examples; max 300 chars/example; max payload 64KB; HTTPS image URL + host allowlist  
**Scale/Scope**: Single feature slice across backend card contract + migration and frontend create/edit/study UI for current user scale (~50 users, ~30 concurrent active)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

Initial Gate Assessment (Pre-Phase 0)

- Architecture gate: PASS. Remains within mandated replicated-monolith stack.
- Infrastructure gate: PASS. No topology change; no infra drift outside Terraform.
- Security gate: PASS. URL hardening (HTTPS + allowlist), plain-text rendering, and optimistic concurrency reduce abuse/overwrite risk without changing auth controls.
- API gate: PASS. REST contract is explicit for create/get/update; no new list endpoints introduced.
- Observability gate: PASS. Validation/conflict failure outcomes are explicitly modeled for logging and diagnostics.
- Data gate: PASS. Additive Flyway migration for Aurora-backed card schema evolution is defined; DynamoDB role remains unchanged.
- Quality gate: PASS. Mandatory tests cover migration, validation, mobile layout, scroll behavior, image fallback, and scheduler invariance.
- Compliance gate: PASS WITH NOTE. Constitution text references SM-2 globally, but this feature does not alter scheduling behavior and explicitly preserves deployed FSRS v6 path; no additional algorithm divergence is introduced by this scope.

Post-Design Re-check (Post-Phase 1)

- Architecture gate: PASS. `research.md`, `data-model.md`, `quickstart.md`, and contracts keep implementation inside existing backend/frontend modules.
- Infrastructure gate: PASS. Design requires no manual cloud-side resources outside normal Terraform governance.
- Security gate: PASS. Contracts enforce allowlisted HTTPS image URLs, payload bounds, plain-text render policy, and stale-write conflict protection.
- API gate: PASS. Contract file defines deterministic request/response and error outcomes (`400`, `409`, `413`) for external behavior.
- Observability gate: PASS. Research/quickstart specify logging focus for validation and version-conflict failures on changed paths.
- Data gate: PASS. Data model documents additive schema defaults and idempotent migration behavior.
- Quality gate: PASS. Quickstart test plan aligns with required regression and UI behavior coverage.
- Compliance gate: PASS WITH NOTE. Post-design artifacts confirm this feature preserves current FSRS v6 scheduling results and does not modify daily-limit/state logic.

## Project Structure

### Documentation (this feature)

```text
specs/009-domain-agnostic-rich-cards/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── rich-cards-api.yaml
└── tasks.md            # generated later by /speckit.tasks
```

### Source Code (repository root)

```text
backend/
├── src/main/java/
│   ├── .../controller/
│   ├── .../service/
│   ├── .../entity/
│   ├── .../repository/
│   └── .../config/
├── src/main/resources/db/migration/
└── src/test/java/
    ├── unit/
    ├── integration/
    └── contract/

frontend/
├── src/
│   ├── features/
│   │   ├── cards/
│   │   └── study/
│   ├── components/
│   ├── services/
│   └── test/
└── tests/e2e/

specs/009-domain-agnostic-rich-cards/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
└── contracts/rich-cards-api.yaml
```

**Structure Decision**: Use existing frontend/backend monolith directories; implement contract/migration changes in backend card modules and UI behavior changes in frontend card/study features without introducing new top-level projects.

## Complexity Tracking

No constitutional violations requiring formal exception tracking for this feature scope.
