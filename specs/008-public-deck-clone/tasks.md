# Tasks: Public Deck Discovery, Personal Study Workspace, and Anki-Style Review Flow

**Input**: Design documents from /specs/008-public-deck-clone/
**Prerequisites**: plan.md (required), spec.md (required), research.md, data-model.md, contracts/public-deck-clone-api.yaml, quickstart.md

**Tests**: This feature changes behavior, persistence, authorization, scheduling, and API contracts; automated tests are required to preserve constitutional quality gates.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing.

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Prepare baseline feature scaffolding for backend, frontend, and migrations.

- [X] T001 Create backend feature package placeholders in backend/src/main/java/com/khaleo/flashcard/service/importmerge/PackageInfo.java
- [X] T002 Create frontend feature module index for public discovery in frontend/src/features/decks-discovery/index.ts
- [X] T003 [P] Create frontend feature module index for study workspace in frontend/src/features/study-workspace/index.ts
- [X] T004 [P] Create frontend feature module index for study session in frontend/src/features/study-session/index.ts
- [X] T005 Create feature migration scaffold for import lineage and conflict tables in backend/src/main/resources/db/migration/V20260319_008__public_clone_merge.sql

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Implement shared domain and infrastructure that all user stories depend on.

**CRITICAL**: No user story implementation starts until this phase is complete.

- [X] T006 Create DeckImportLink entity in backend/src/main/java/com/khaleo/flashcard/entity/DeckImportLink.java
- [X] T007 [P] Create ReimportMergeConflict entity in backend/src/main/java/com/khaleo/flashcard/entity/ReimportMergeConflict.java
- [X] T008 [P] Create DeckImportLink repository in backend/src/main/java/com/khaleo/flashcard/repository/DeckImportLinkRepository.java
- [X] T009 [P] Create ReimportMergeConflict repository in backend/src/main/java/com/khaleo/flashcard/repository/ReimportMergeConflictRepository.java
- [X] T010 Implement shared pagination constraints for deck list endpoints in backend/src/main/java/com/khaleo/flashcard/config/PaginationConfig.java
- [X] T011 Implement shared authorization guard for private deck ownership in backend/src/main/java/com/khaleo/flashcard/service/deck/DeckAuthorizationService.java
- [X] T012 Implement shared telemetry event helpers for discovery/import/merge/scheduling in backend/src/main/java/com/khaleo/flashcard/config/FeatureTelemetryLogger.java
- [X] T055 Implement shared email-verification guard for learning/import workflows in backend/src/main/java/com/khaleo/flashcard/service/auth/VerifiedAccountGuard.java
- [X] T056 Add New Relic instrumentation coverage for discovery/import/merge runtime paths in backend/src/main/java/com/khaleo/flashcard/config/observability/NewRelicDeckMediaInstrumentation.java

**Checkpoint**: Foundational layer complete; user stories can begin.

---

## Phase 3: User Story 1 - Keep Study Workspace Personal (Priority: P1) 🎯 MVP

**Goal**: Ensure study/cards workspace lists and mutations are restricted to learner-owned private decks only.

**Independent Test**: With two users and mixed deck ownership, verify one user can only see and mutate their own private decks while non-owned mutations are denied.

### Tests for User Story 1

- [X] T013 [P] [US1] Add contract test for private workspace list ownership in backend/src/test/java/com/khaleo/flashcard/contract/PrivateWorkspaceDeckContractTest.java
- [X] T014 [P] [US1] Add integration test for owner-only CRUD and search in backend/src/test/java/com/khaleo/flashcard/integration/PrivateDeckOwnershipIntegrationTest.java
- [X] T015 [P] [US1] Add frontend integration test for Study/Cards private filtering in frontend/src/test/study-workspace/privateWorkspaceVisibility.test.tsx

### Implementation for User Story 1

- [X] T016 [P] [US1] Implement private workspace deck query filtered by owner and visibility in backend/src/main/java/com/khaleo/flashcard/repository/DeckRepository.java
- [X] T017 [US1] Implement study workspace service for owner-only list/search in backend/src/main/java/com/khaleo/flashcard/service/deck/PrivateWorkspaceService.java
- [X] T018 [US1] Implement owner-guarded private deck CRUD orchestration in backend/src/main/java/com/khaleo/flashcard/service/deck/PrivateDeckCrudService.java
- [X] T019 [US1] Implement private workspace API endpoints in backend/src/main/java/com/khaleo/flashcard/controller/deck/PrivateWorkspaceController.java
- [X] T020 [US1] Implement authorization-denied error mapping for private workspace flows in backend/src/main/java/com/khaleo/flashcard/config/GlobalExceptionHandler.java
- [X] T021 [P] [US1] Implement study workspace data client in frontend/src/services/privateWorkspaceApi.ts
- [X] T022 [US1] Implement Study tab private deck list and search UI in frontend/src/features/study-workspace/StudyWorkspacePage.tsx
- [X] T023 [US1] Implement Cards tab private deck CRUD/search actions in frontend/src/features/cards-workspace/CardsWorkspacePage.tsx

**Checkpoint**: User Story 1 is independently functional and testable.

---

## Phase 4: User Story 2 - Discover Public Decks and Import as Private Copy (Priority: P1)

**Goal**: Support guest-readable public discovery and authenticated import/re-import merge into learner private copies.

**Independent Test**: As guest, browse public decks only and fail import; as authenticated user, import and re-import a deck with deterministic merge/conflict behavior.

### Tests for User Story 2

- [X] T024 [P] [US2] Add contract test for GET /public/decks pagination and visibility in backend/src/test/java/com/khaleo/flashcard/contract/PublicDeckDiscoveryContractTest.java
- [X] T025 [P] [US2] Add contract test for POST /public/decks/{deckId}/import auth gate in backend/src/test/java/com/khaleo/flashcard/contract/PublicDeckImportAuthContractTest.java
- [X] T026 [P] [US2] Add integration test for first import copy semantics in backend/src/test/java/com/khaleo/flashcard/integration/PublicDeckImportIntegrationTest.java
- [X] T027 [P] [US2] Add integration test for re-import merge with no conflicts in backend/src/test/java/com/khaleo/flashcard/integration/ReimportMergeNoConflictIntegrationTest.java
- [X] T028 [P] [US2] Add integration test for re-import conflict decision path in backend/src/test/java/com/khaleo/flashcard/integration/ReimportMergeConflictResolutionIntegrationTest.java
- [X] T029 [P] [US2] Add frontend test for guest browse and login-required import action in frontend/src/test/decks-discovery/publicDiscoveryAuthFlow.test.tsx
- [X] T057 [P] [US2] Add integration test for re-import when source deck is deleted or made private mid-flow in backend/src/test/java/com/khaleo/flashcard/integration/ReimportSourceStateChangeIntegrationTest.java
- [X] T058 [P] [US2] Add contract test for authenticated-but-unverified import denial in backend/src/test/java/com/khaleo/flashcard/contract/PublicDeckImportVerificationContractTest.java

### Implementation for User Story 2

- [X] T030 [P] [US2] Implement public deck catalog query (PUBLIC visibility only) in backend/src/main/java/com/khaleo/flashcard/repository/DeckRepository.java
- [X] T031 [US2] Implement public discovery service with deterministic pagination defaults/max in backend/src/main/java/com/khaleo/flashcard/service/deck/PublicDeckDiscoveryService.java
- [X] T032 [US2] Implement discovery controller endpoints from contract in backend/src/main/java/com/khaleo/flashcard/controller/deck/PublicDeckController.java
- [X] T033 [US2] Implement import copy service (metadata/cards/media refs, no progress/history) in backend/src/main/java/com/khaleo/flashcard/service/importmerge/PublicDeckImportService.java
- [X] T034 [US2] Implement re-import merge service with no-conflict auto-merge behavior in backend/src/main/java/com/khaleo/flashcard/service/importmerge/ReimportMergeService.java
- [X] T035 [US2] Implement conflict listing and resolve endpoints in backend/src/main/java/com/khaleo/flashcard/controller/deck/ImportConflictController.java
- [X] T036 [US2] Implement conflict resolution application logic (LOCAL/CLOUD) in backend/src/main/java/com/khaleo/flashcard/service/importmerge/ConflictResolutionService.java
- [X] T037 [P] [US2] Implement frontend discovery page with import CTA and auth redirect in frontend/src/features/decks-discovery/DecksDiscoveryPage.tsx
- [X] T038 [US2] Implement frontend re-import conflict resolution dialog and action flow in frontend/src/features/decks-discovery/ReimportConflictDialog.tsx
- [X] T059 [US2] Enforce verified-account gate on import and re-import endpoints in backend/src/main/java/com/khaleo/flashcard/controller/deck/PublicDeckController.java

**Checkpoint**: User Story 2 is independently functional and testable.

---

## Phase 5: User Story 3 - Study in Two-Sided Flashcard Mode With Anki-Style Timing (Priority: P2)

**Goal**: Deliver two-sided session flow and preserve Anki-style scheduling with required new-card first-step timings.

**Independent Test**: Start a session from learner private deck, flip card sides, submit Again/Hard/Good/Easy ratings, and verify expected due-time mapping for new cards.

### Tests for User Story 3

- [X] T039 [P] [US3] Add unit tests for new-card first-step timing mapping in backend/src/test/java/com/khaleo/flashcard/unit/StudyTimingMappingUnitTest.java
- [X] T040 [P] [US3] Add integration test for study session rating response and queue advance in backend/src/test/java/com/khaleo/flashcard/integration/StudySessionFlowIntegrationTest.java
- [X] T041 [P] [US3] Add integration test preserving non-new Anki-style behavior in backend/src/test/java/com/khaleo/flashcard/integration/StudySchedulerLegacyBehaviorIntegrationTest.java
- [X] T042 [P] [US3] Add frontend interaction test for two-sided flashcard flow in frontend/src/test/study-session/twoSidedFlashcardFlow.test.tsx

### Implementation for User Story 3

- [X] T043 [US3] Implement explicit new-card first-step timing strategy in backend/src/main/java/com/khaleo/flashcard/service/study/StudyTimingPolicy.java
- [X] T044 [US3] Integrate timing policy into scheduler rating handler in backend/src/main/java/com/khaleo/flashcard/service/study/StudySchedulerService.java
- [X] T045 [US3] Preserve account-level daily limit checks for session entry in backend/src/main/java/com/khaleo/flashcard/service/study/StudySessionLimitService.java
- [X] T046 [US3] Implement session API response contract for current side and rating actions in backend/src/main/java/com/khaleo/flashcard/controller/study/StudySessionController.java
- [X] T047 [P] [US3] Implement frontend study session API client in frontend/src/services/studySessionApi.ts
- [X] T048 [US3] Implement two-sided flashcard session UI with rating controls in frontend/src/features/study-session/StudySessionPage.tsx
- [X] T060 [US3] Enforce verified-account gate on study session endpoints in backend/src/main/java/com/khaleo/flashcard/controller/study/StudySessionController.java

**Checkpoint**: User Story 3 is independently functional and testable.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Finalize quality, observability, documentation, and end-to-end validation across stories.

- [X] T049 [P] Add structured observability assertions for discovery/import/merge/scheduling logs in backend/src/test/java/com/khaleo/flashcard/integration/FeatureTelemetryIntegrationTest.java
- [X] T050 [P] Add Playwright end-to-end flow for guest browse, login import, and study access in frontend/tests/e2e/public-discovery-private-study.e2e.ts
- [X] T051 Validate OpenAPI contract parity against implemented controllers in specs/008-public-deck-clone/contracts/public-deck-clone-api.yaml
- [X] T052 Run and document quickstart verification evidence in specs/008-public-deck-clone/quickstart.md
- [X] T053 Update feature-oriented backend test run documentation in backend/README.md
- [X] T054 Update frontend workflow notes for discovery/workspace/session tabs in frontend/README.md
- [X] T061 Add CloudWatch alarm definitions for discovery/import/merge and study 5xx/error-rate paths in infra/terraform/cloudwatch-persistence-alarms.tf
- [X] T062 Add performance validation suite for SC-003 and SC-006 latency targets in backend/src/test/java/com/khaleo/flashcard/integration/FeaturePerformanceValidationIT.java
- [X] T063 Add modular-boundary architecture guardrail note for flashcard-scoped modules in specs/008-public-deck-clone/quickstart.md

---

## Dependencies & Execution Order

### Phase Dependencies

- Phase 1 (Setup): No dependencies.
- Phase 2 (Foundational): Depends on Phase 1 and blocks all user stories.
- Phase 3 (US1): Depends on Phase 2 completion.
- Phase 4 (US2): Depends on Phase 2 completion.
- Phase 5 (US3): Depends on Phase 2 and should be integrated after US1 workspace behavior is available.
- Phase 6 (Polish): Depends on completion of selected user stories.

Additional sequencing note:
- T016 and T030 both modify backend/src/main/java/com/khaleo/flashcard/repository/DeckRepository.java and should be implemented in one coordinated change to avoid merge conflicts.

### User Story Dependencies

- US1 (P1): No dependency on other stories after foundational phase.
- US2 (P1): No dependency on other stories after foundational phase.
- US3 (P2): Depends on private workspace semantics from US1 for valid study source decks.

### Within Each User Story

- Tests should be authored before implementation and must fail before corresponding code changes.
- Repository/entity updates precede service logic.
- Service logic precedes controller/API integration.
- Backend API readiness precedes frontend integration for that story.

## Parallel Execution Examples

### User Story 1

- Execute T013, T014, and T015 in parallel (different test files).
- Execute T021 in parallel with backend tasks T017-T020 after T016 is complete.

### User Story 2

- Execute T024, T025, T026, T027, T028, and T029 in parallel (independent tests).
- Execute T030 and T037 in parallel after foundational phase.

### User Story 3

- Execute T039, T040, T041, and T042 in parallel.
- Execute T047 in parallel with backend implementation T043-T046 once API shape stabilizes.

## Implementation Strategy

### MVP First (US1)

1. Complete Phase 1 and Phase 2.
2. Deliver Phase 3 (US1) fully.
3. Validate US1 independently before proceeding.

### Incremental Delivery

1. US1 for private workspace boundaries and owner-safe mutations.
2. US2 for public discovery and import/re-import merge conflict flow.
3. US3 for study session timing and two-sided interaction.
4. Polish with telemetry, E2E, and docs updates.

### Team Parallelization

1. One engineer handles backend foundational entities/repositories (T006-T012).
2. One engineer handles frontend workspace/discovery scaffolding (T002-T004, then US tasks).
3. After foundational completion, split by story owners (US1, US2, US3) while coordinating API contracts.
