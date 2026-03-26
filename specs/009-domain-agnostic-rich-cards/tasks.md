# Tasks: Domain-Agnostic Rich Card Content and Study UI Layout

**Input**: Design documents from `/specs/009-domain-agnostic-rich-cards/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/rich-cards-api.yaml, quickstart.md

**Tests**: Include automated tests required by spec and constitution (migration, validation, layout behavior, image fallback, and FSRS v6 non-regression).

**Organization**: Tasks are grouped by user story so each story can be implemented and tested independently.

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Prepare shared configuration and implementation scaffolding used by all stories.

- [X] T001 Add rich-card feature configuration placeholders in backend/src/main/resources/application.yml
- [X] T002 [P] Add frontend rich-card constants (examples limit, per-item length, payload guard) in frontend/src/features/cards/richCardConfig.ts
- [X] T003 [P] Add shared backend rich-card test fixture builder in backend/src/test/java/com/khaleo/flashcard/integration/support/RichCardFixtures.java

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core model, migration, contract, and shared validation/error infrastructure required before story work.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

- [X] T004 Create additive Flyway migration for rich-card columns/defaults in backend/src/main/resources/db/migration/V20260326_012__rich_card_content.sql
- [X] T005 Update card persistence model with rich fields and version token in backend/src/main/java/com/khaleo/flashcard/entity/Card.java
- [X] T006 Update rich-card create/update request DTOs in backend/src/main/java/com/khaleo/flashcard/controller/card/dto/CreateCardRequest.java
- [X] T007 Update rich-card update DTO with full-replace + version semantics in backend/src/main/java/com/khaleo/flashcard/controller/card/dto/UpdateCardRequest.java
- [X] T008 Update card response DTO for term/answer/metadata/examples projection in backend/src/main/java/com/khaleo/flashcard/controller/card/dto/CardResponse.java
- [X] T009 Implement shared rich-card validation rules (HTTPS allowlist, examples constraints, payload size) in backend/src/main/java/com/khaleo/flashcard/service/persistence/RelationalPersistenceService.java
- [X] T010 Add deterministic validation and version-conflict error mapping in backend/src/main/java/com/khaleo/flashcard/controller/GlobalExceptionHandler.java
- [X] T011 [P] Update frontend API contract types for rich-card payload and version token in frontend/src/services/contracts/dto.ts
- [X] T012 [P] Update private workspace card API client for full-replace contract in frontend/src/services/privateWorkspaceApi.ts
- [X] T013 [P] Add telemetry fields for rich-card validation/conflict outcomes in backend/src/main/java/com/khaleo/flashcard/config/FeatureTelemetryLogger.java

**Checkpoint**: Foundation ready - user story implementation can now begin.

---

## Phase 3: User Story 1 - Author Rich Cards in Create/Edit Flow (Priority: P1) 🎯 MVP

**Goal**: Enable create/edit with domain-agnostic rich-card fields and strict validation behavior.

**Independent Test**: Author can create/edit cards with valid rich payload, blocked by field validation when invalid, and see null metadata hidden in preview.

### Tests for User Story 1

- [X] T014 [P] [US1] Add backend contract tests for rich-card create/update validation outcomes in backend/src/test/java/com/khaleo/flashcard/contract/RichCardContractTest.java
- [X] T015 [P] [US1] Add backend integration tests for migration defaults and idempotency in backend/src/test/java/com/khaleo/flashcard/integration/RichCardMigrationIT.java
- [X] T016 [P] [US1] Add backend integration tests for optimistic concurrency (`409`) on stale updates in backend/src/test/java/com/khaleo/flashcard/integration/RichCardConcurrencyIT.java
- [X] T017 [P] [US1] Add frontend unit tests for create/edit validator and examples limits in frontend/src/test/cards/richCardEditor.validation.test.tsx
- [X] T018 [P] [US1] Add frontend contract tests for rich-card DTO mapping in frontend/src/services/contracts/richCard.contract.test.ts

### Implementation for User Story 1

- [X] T019 [US1] Refactor card create/update endpoints to use rich-card payload contract in backend/src/main/java/com/khaleo/flashcard/controller/card/CardController.java
- [X] T020 [US1] Implement rich-card persistence mapping and full-replace update behavior in backend/src/main/java/com/khaleo/flashcard/service/persistence/RelationalPersistenceService.java
- [X] T021 [US1] Implement card editor form with term/answer/imageUrl/partOfSpeech/phonetic/examples fields in frontend/src/features/cards/CardsPage.tsx
- [X] T022 [US1] Implement row-based examples input component (add/remove with max 20) in frontend/src/features/cards/RichExamplesInput.tsx
- [X] T023 [US1] Implement create/edit preview that hides null metadata rows in frontend/src/features/cards/RichCardPreview.tsx
- [X] T024 [US1] Wire cards feature hooks to rich-card API contract and version token in frontend/src/features/cards/useCards.ts
- [X] T025 [US1] Update application card type model to domain-agnostic rich fields in frontend/src/types.ts
- [X] T026 [US1] Add structured validation/conflict logging for create/update failures in backend/src/main/java/com/khaleo/flashcard/controller/card/CardController.java

**Checkpoint**: User Story 1 should be fully functional and independently testable.

---

## Phase 4: User Story 2 - Study Rich Cards in Focused Layout (Priority: P1)

**Goal**: Render rich-card content in study mode with required ordering, examples-only scrolling, and mobile-safe rating controls.

**Independent Test**: Learner can study rich cards where core content stays visible, only examples scroll, rating buttons appear only after flip, and image fallback works.

### Tests for User Story 2

- [X] T027 [P] [US2] Add frontend unit tests for study card layout ordering and null-metadata hiding in frontend/src/test/study-session/richCardLayout.test.tsx
- [X] T028 [P] [US2] Add frontend unit tests for examples-scroll reset on flip in frontend/src/test/study-session/richCardScrollReset.test.tsx
- [X] T029 [P] [US2] Add frontend unit tests for image error fallback rendering in frontend/src/test/study-session/richCardImageFallback.test.tsx
- [X] T030 [P] [US2] Add Playwright mobile E2E for fixed bottom rating controls with safe-area behavior in frontend/tests/e2e/study-session-mobile-rating-bar.e2e.ts

### Implementation for User Story 2

- [X] T031 [US2] Refactor study session page to rich-card content order and back-face-only ratings in frontend/src/features/study-session/StudySessionPage.tsx
- [X] T032 [US2] Implement dedicated examples scroll container with reset-on-flip behavior in frontend/src/features/study-session/RichCardContent.tsx
- [X] T033 [US2] Implement image component with placeholder + error icon fallback in frontend/src/components/StudyCardImage.tsx
- [X] T034 [US2] Add mobile fixed-bottom rating bar styles with safe-area inset support in frontend/src/index.css
- [X] T035 [US2] Remove redundant helper text and keep minimal first-use/empty-state guidance in frontend/src/features/study-session/StudySessionPage.tsx
- [X] T036 [US2] Update study session API DTO mapping for rich-card response fields in frontend/src/services/studySessionApi.ts

**Checkpoint**: User Stories 1 and 2 work independently.

---

## Phase 5: User Story 3 - Keep Rating Behavior and FSRS v6 Logic Intact (Priority: P2)

**Goal**: Preserve existing FSRS v6 scheduling outputs while introducing rich-card content fields.

**Independent Test**: Existing FSRS v6 rating inputs produce unchanged scheduling outputs before/after rich-card rollout.

### Tests for User Story 3

- [X] T037 [P] [US3] Extend FSRS v6 regression unit coverage for unchanged interval/difficulty/stability outcomes in backend/src/test/java/com/khaleo/flashcard/unit/study/SpacedRepetitionServiceTest.java
- [X] T038 [P] [US3] Extend study scheduling integration non-regression assertions in backend/src/test/java/com/khaleo/flashcard/integration/StudySchedulerLegacyBehaviorIntegrationTest.java
- [X] T039 [P] [US3] Extend rate-card contract tests to confirm unchanged scheduling fields in backend/src/test/java/com/khaleo/flashcard/contract/study/StudyRateCardContractTest.java
- [X] T040 [P] [US3] Add frontend regression test for back-face-only rating visibility with rich-card content in frontend/src/test/study-session/ratingVisibility.richCard.test.tsx

### Implementation for User Story 3

- [X] T041 [US3] Ensure study session backend card projection additions do not alter FSRS computation path in backend/src/main/java/com/khaleo/flashcard/service/study/NextCardsService.java
- [X] T042 [US3] Verify rating endpoint path remains algorithm-invariant while carrying rich content in backend/src/main/java/com/khaleo/flashcard/controller/study/StudySessionController.java
- [X] T043 [US3] Add explicit FSRS v6 invariance notes to test fixtures/baselines in backend/src/test/java/com/khaleo/flashcard/integration/support/IntegrationPersistenceTestBase.java

**Checkpoint**: All user stories are functional and independently testable.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Final hardening, documentation, and verification across stories.

- [X] T044 [P] Update rich-card API documentation and examples in specs/009-domain-agnostic-rich-cards/contracts/rich-cards-api.yaml
- [X] T045 [P] Add implementation notes and verification evidence checklist in specs/009-domain-agnostic-rich-cards/quickstart.md
- [X] T046 Run backend targeted test suite for migration, validation, concurrency, and FSRS non-regression in backend/pom.xml
- [X] T047 Run frontend unit + E2E verification for rich-card authoring/study behavior in frontend/package.json
- [X] T048 Validate telemetry/log outputs for validation conflicts and study path continuity in backend/src/main/java/com/khaleo/flashcard/config/FeatureTelemetryLogger.java

---

## Dependencies & Execution Order

### Phase Dependencies

- Phase 1 (Setup): no dependencies.
- Phase 2 (Foundational): depends on Phase 1 and blocks all user stories.
- Phase 3 (US1), Phase 4 (US2), Phase 5 (US3): depend on Phase 2 completion.
- Phase 6 (Polish): depends on all selected user stories being complete.

### User Story Dependencies

- US1 (P1): starts immediately after Foundational.
- US2 (P1): starts immediately after Foundational; consumes rich-card payload from foundation and can run in parallel with US1 implementation work.
- US3 (P2): starts after Foundational; validates non-regression and can run in parallel once baseline rich-card contract changes are in place.

### Within Each User Story

- Write tests first and confirm they fail for the new behavior.
- Complete backend contract/domain updates before frontend wiring for the same story.
- Complete core implementation before cross-cutting logging/polish tasks.

## Parallel Opportunities

- Setup: T002 and T003 can run in parallel.
- Foundational: T011, T012, and T013 can run in parallel after T004-T010 start.
- US1 tests: T014-T018 can run in parallel.
- US2 tests: T027-T030 can run in parallel.
- US3 tests: T037-T040 can run in parallel.
- Polish: T044 and T045 can run in parallel.

## Parallel Example: User Story 1

```bash
# Parallel backend/frontend tests for US1
T014 + T015 + T016 + T017 + T018

# Parallel UI components after API contract is stable
T022 + T023
```

## Parallel Example: User Story 2

```bash
# Parallel test creation for study UI behavior
T027 + T028 + T029 + T030

# Parallel implementation split between component and styling
T033 + T034
```

## Parallel Example: User Story 3

```bash
# Parallel regression test updates
T037 + T038 + T039 + T040
```

## Implementation Strategy

### MVP First (US1)

1. Complete Phase 1 and Phase 2.
2. Complete US1 (Phase 3).
3. Validate US1 independently before expanding study UX.

### Incremental Delivery

1. Foundation first (Phases 1-2).
2. Deliver US1 (authoring contract + migration).
3. Deliver US2 (study rendering/UX behavior).
4. Deliver US3 (algorithm invariance confirmation).
5. Finalize with Phase 6 hardening and verification.

### Parallel Team Strategy

1. Team completes Setup + Foundational together.
2. After foundation:
   - Engineer A: US1 backend + contract tests.
   - Engineer B: US2 study UI + E2E.
   - Engineer C: US3 FSRS non-regression suite.
3. Merge at Phase 6 with full quickstart verification.
