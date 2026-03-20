# Implementation Plan: Public Deck Discovery, Personal Study Workspace, and Anki-Style Review Flow

**Branch**: `008-public-deck-clone` | **Date**: 2026-03-19 | **Spec**: `specs/008-public-deck-clone/spec.md`
**Input**: Feature specification from `/specs/008-public-deck-clone/spec.md`

**Note**: This template is filled in by the `/speckit.plan` command. See `.specify/templates/plan-template.md` for the execution workflow.

## Summary

Deliver a user-facing separation between discovery and personal study: `Decks` lists
public decks (guest-readable), `Study/Cards` lists only learner-owned private decks,
and importing a public deck creates/updates a private copy with cards and media
references while excluding learning progress/history. Re-importing the same source
uses merge semantics, preserving local edits by default where no conflict exists and
requiring explicit user conflict choice (local vs cloud) when conflicts occur. Study
session presentation remains two-sided flashcard flow while preserving constitution-
mandated Anki-style scheduling fidelity and account-level learning limits.

## Technical Context

<!--
  ACTION REQUIRED: Replace the content in this section with the technical details
  for the project. The structure here is presented in advisory capacity to guide
  the iteration process.
-->

**Language/Version**: Java 17 (Spring Boot backend), TypeScript (React frontend), Terraform HCL  
**Primary Dependencies**: Spring Web/Security/Data JPA, Hibernate, Flyway, React, Tailwind CSS, AWS SDK v2, JWT auth stack  
**Storage**: Aurora MySQL for transactional deck/card/scheduling state; DynamoDB for activity/event logs  
**Testing**: JUnit 5 + Spring integration tests + MockMvc contract tests; Vitest/React Testing Library + Playwright for frontend flows  
**Target Platform**: AWS-hosted web application (CloudFront/S3 frontend + EC2 private-subnet backend behind ALB/WAF)
**Project Type**: Web application (replicated monolith with frontend, backend, and Terraform-managed infrastructure)  
**Performance Goals**: Discovery and study list first-page responses p95 < 1s; import/re-import completion p95 < 30s for typical deck sizes; rating response p95 < 1s  
**Constraints**: Preserve SM-2/Anki scheduling fidelity, enforce owner-based mutation rules, guest-read/public-only discovery, mandatory pagination on list endpoints  
**Scale/Scope**: Portfolio scale (~50 users, ~30 concurrent active users); one new end-to-end feature slice across discovery, import merge, and study session behavior

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

Initial Gate Assessment (Pre-Phase 0)

- Architecture gate: PASS. Design remains in the existing replicated monolith and
  mandated stack (React + Tailwind; Java 17 + Spring Boot + Hibernate + Flyway;
  Aurora + DynamoDB).
- Infrastructure gate: PASS. No topology deviation is required; any infra deltas
  (if needed for indexing/alarms) remain Terraform-only.
- Security gate: PASS. Guest read access is limited to public deck discovery;
  import/copy and all private deck mutations stay authenticated/authorized.
- API gate: PASS. Discovery/study/card list flows require explicit pagination
  semantics; import/re-import remain RESTful actions.
- Observability gate: PASS. Feature requires structured telemetry for discovery,
  import/re-import merge, conflict decisions, authorization denials, and study
  scheduling outcomes.
- Data gate: PASS. Aurora remains authoritative for deck/card/scheduling state;
  no change to DynamoDB role beyond existing activity/event logging.
- Quality gate: PASS. Plan includes layered test coverage for role boundaries,
  import merge/conflict outcomes, and scheduling/timing correctness.
- Compliance gate: PASS. New-card timing requirements and SM-2 fidelity are
  explicitly preserved; account-level daily limits remain unchanged.

Post-Design Re-check (Post-Phase 1)

- Architecture gate: PASS. `research.md`, `data-model.md`, and contracts keep
  implementation in existing frontend/backend modules without stack deviation.
- Infrastructure gate: PASS. `quickstart.md` and contracts do not require manual
  AWS console drift; Terraform remains source of truth for any environment changes.
- Security gate: PASS. Design preserves guest/public read-only behavior, auth-gated
  import, owner-only private mutations, and explicit deny paths.
- API gate: PASS. Contracts define paginated list endpoints and deterministic
  merge/conflict decision paths.
- Observability gate: PASS. Artifacts define logs/metrics for conflict prompts,
  merge outcomes, import failures, and schedule actions.
- Data gate: PASS. Data model separates source lineage, private copy state, and
  conflict-decision audit artifacts within Aurora-domain ownership.
- Quality gate: PASS. Test plan covers unit/integration/E2E split aligned with
  80% and 60/30/10 governance goals.
- Compliance gate: PASS. Post-design artifacts keep Anki/SM-2 fidelity and daily-
  limit behavior intact.

## Project Structure

### Documentation (this feature)

```text
specs/[###-feature]/
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (/speckit.plan command)
├── data-model.md        # Phase 1 output (/speckit.plan command)
├── quickstart.md        # Phase 1 output (/speckit.plan command)
├── contracts/           # Phase 1 output (/speckit.plan command)
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
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
│   │   ├── deck/
│   │   ├── card/
│   │   └── study/
│   ├── service/
│   │   ├── deck/
│   │   ├── card/
│   │   ├── study/
│   │   └── importmerge/
│   ├── repository/
│   ├── entity/
│   └── config/
└── src/test/java/com/khaleo/flashcard/
  ├── contract/
  ├── integration/
  └── unit/

frontend/
├── src/
│   ├── features/
│   │   ├── decks-discovery/
│   │   ├── study-workspace/
│   │   ├── cards-workspace/
│   │   └── study-session/
│   ├── components/
│   ├── router/
│   └── services/
└── src/test/

infra/
└── terraform/

specs/008-public-deck-clone/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
└── contracts/
```

**Structure Decision**: Use the existing web-application monolith layout, adding
feature-specific behavior under `frontend/src/features/` and backend domain/service
modules without introducing new top-level projects or architectural divergence.

## Complexity Tracking

No constitutional violations requiring justification.
