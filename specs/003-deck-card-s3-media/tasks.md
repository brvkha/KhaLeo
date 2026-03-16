# Tasks: Deck/Card Management and Media Uploads

**Input**: Design documents from `/specs/003-deck-card-s3-media/`
**Prerequisites**: plan.md (required), spec.md (required), research.md, data-model.md, contracts/deck-card-media-contract.md, quickstart.md

**Tests**: Include unit, integration, and contract tests to satisfy constitutional quality gates for persistence, authorization, media upload security, observability, and runtime integrations.

**Organization**: Tasks are grouped by user story so each story can be implemented and validated independently.

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Prepare dependency/config scaffolding for deck-card-media implementation.

- [X] T001 Add S3 and S3 presigner dependencies in backend/pom.xml
- [X] T002 Add media authorization configuration properties in backend/src/main/resources/application.yml
- [X] T003 [P] Create media package scaffolding in backend/src/main/java/com/khaleo/flashcard/service/media/package-info.java and backend/src/main/java/com/khaleo/flashcard/config/media/package-info.java
- [X] T004 [P] Create deck/media test package scaffolding in backend/src/test/java/com/khaleo/flashcard/contract/deckmedia/package-info.java, backend/src/test/java/com/khaleo/flashcard/integration/deckmedia/package-info.java, and backend/src/test/java/com/khaleo/flashcard/unit/deckmedia/package-info.java

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Build blocking domain, persistence, authorization, and observability capabilities required before user stories.

**CRITICAL**: No user story work starts before this phase is complete.

- [X] T005 Create Flyway migration for media authorization and media reference tables in backend/src/main/resources/db/migration/V3__deck_card_media_schema.sql
- [X] T006 [P] Create MediaUploadAuthorization entity in backend/src/main/java/com/khaleo/flashcard/entity/MediaUploadAuthorization.java
- [X] T007 [P] Create MediaObjectReference entity in backend/src/main/java/com/khaleo/flashcard/entity/MediaObjectReference.java
- [X] T008 [P] Create MediaUploadAuthorizationRepository in backend/src/main/java/com/khaleo/flashcard/repository/MediaUploadAuthorizationRepository.java
- [X] T009 [P] Create MediaObjectReferenceRepository in backend/src/main/java/com/khaleo/flashcard/repository/MediaObjectReferenceRepository.java
- [X] T010 [P] Extend deck and card repository pagination/search interfaces in backend/src/main/java/com/khaleo/flashcard/repository/DeckRepository.java and backend/src/main/java/com/khaleo/flashcard/repository/CardRepository.java
- [X] T011 Implement owner/admin deck-card access guard service in backend/src/main/java/com/khaleo/flashcard/service/persistence/DeckCardAccessGuard.java
- [X] T012 [P] Add deck/card/media error codes to persistence validation model in backend/src/main/java/com/khaleo/flashcard/service/persistence/PersistenceValidationException.java
- [X] T013 Update exception mapper for deck/card/media validation and authorization failures in backend/src/main/java/com/khaleo/flashcard/service/persistence/PersistenceValidationExceptionMapper.java
- [X] T014 [P] Add New Relic deck/media instrumentation helper in backend/src/main/java/com/khaleo/flashcard/config/observability/NewRelicDeckMediaInstrumentation.java
- [X] T015 Implement media request rate limiter service (30 requests per user per minute) in backend/src/main/java/com/khaleo/flashcard/service/media/MediaAuthorizationRateLimiter.java
- [X] T016 Implement media reference tracking/deletion coordinator in backend/src/main/java/com/khaleo/flashcard/service/media/MediaReferenceService.java

**Checkpoint**: Foundation ready for user-story implementation.

---

## Phase 3: User Story 1 - Manage Decks Securely (Priority: P1) 🎯 MVP

**Goal**: Deliver secure deck CRUD with public/private read behavior and pagination/filtering.

**Independent Test**: Create/list/get/update/delete decks, validate pagination/filter behavior, and verify unauthorized mutation denial while public decks remain readable.

### Tests for User Story 1

- [X] T017 [P] [US1] Add contract tests for POST/GET/PUT/DELETE deck endpoints in backend/src/test/java/com/khaleo/flashcard/contract/deckmedia/DeckManagementContractTest.java
- [X] T018 [P] [US1] Add integration tests for deck CRUD and owner/admin mutation checks in backend/src/test/java/com/khaleo/flashcard/integration/deckmedia/DeckManagementAuthorizationIT.java
- [X] T019 [P] [US1] Add integration tests for deck list pagination and filter behavior in backend/src/test/java/com/khaleo/flashcard/integration/deckmedia/DeckPaginationFilterIT.java
- [X] T020 [P] [US1] Add unit tests for deck access guard decisions in backend/src/test/java/com/khaleo/flashcard/unit/deckmedia/DeckCardAccessGuardTest.java

### Implementation for User Story 1

- [X] T021 [P] [US1] Create deck request/response DTOs in backend/src/main/java/com/khaleo/flashcard/controller/deck/dto/CreateDeckRequest.java, backend/src/main/java/com/khaleo/flashcard/controller/deck/dto/UpdateDeckRequest.java, and backend/src/main/java/com/khaleo/flashcard/controller/deck/dto/DeckResponse.java
- [X] T022 [P] [US1] Create paginated response DTO for deck/card lists in backend/src/main/java/com/khaleo/flashcard/controller/common/PagedResponse.java
- [X] T023 [US1] Implement deck list/detail/update/delete service methods in backend/src/main/java/com/khaleo/flashcard/service/persistence/RelationalPersistenceService.java
- [X] T024 [US1] Implement deck REST controller for `/api/v1/decks` in backend/src/main/java/com/khaleo/flashcard/controller/deck/DeckController.java
- [X] T025 [US1] Enforce public/private deck read and owner/admin mutation checks in backend/src/main/java/com/khaleo/flashcard/service/persistence/DeckCardAccessGuard.java
- [X] T026 [US1] Add deck operation observability events in backend/src/main/java/com/khaleo/flashcard/config/observability/NewRelicDeckMediaInstrumentation.java

**Checkpoint**: User Story 1 is independently functional and testable.

---

## Phase 4: User Story 2 - Build and Find Cards in a Deck (Priority: P2)

**Goal**: Deliver card create/update/delete and deck-scoped search with clarified matching semantics and pagination.

**Independent Test**: Add/update/delete cards and verify search by front/back contains + vocabulary exact in deck scope with paged responses.

### Tests for User Story 2

- [X] T027 [P] [US2] Add contract tests for card CRUD and search endpoints in backend/src/test/java/com/khaleo/flashcard/contract/deckmedia/CardManagementSearchContractTest.java
- [X] T028 [P] [US2] Add integration tests for card CRUD owner/admin rules in backend/src/test/java/com/khaleo/flashcard/integration/deckmedia/CardManagementAuthorizationIT.java
- [X] T029 [P] [US2] Add integration tests for deck-scoped search semantics and pagination in backend/src/test/java/com/khaleo/flashcard/integration/deckmedia/CardSearchSemanticsIT.java
- [X] T030 [P] [US2] Add unit tests for card search criteria normalization in backend/src/test/java/com/khaleo/flashcard/unit/deckmedia/CardSearchCriteriaTest.java

### Implementation for User Story 2

- [X] T031 [P] [US2] Create card request/response/search DTOs in backend/src/main/java/com/khaleo/flashcard/controller/card/dto/CreateCardRequest.java, backend/src/main/java/com/khaleo/flashcard/controller/card/dto/UpdateCardRequest.java, backend/src/main/java/com/khaleo/flashcard/controller/card/dto/CardResponse.java, and backend/src/main/java/com/khaleo/flashcard/controller/card/dto/CardSearchQuery.java
- [X] T032 [US2] Implement card update/delete and search service methods in backend/src/main/java/com/khaleo/flashcard/service/persistence/RelationalPersistenceService.java
- [X] T033 [US2] Implement card REST controller for `/api/v1/decks/{deckId}/cards/*` and `/api/v1/cards/{id}` in backend/src/main/java/com/khaleo/flashcard/controller/card/CardController.java
- [X] T034 [US2] Implement repository query/specification logic for case-insensitive front/back contains and case-insensitive vocabulary exact in backend/src/main/java/com/khaleo/flashcard/repository/CardRepository.java
- [X] T035 [US2] Apply private/public read checks for card reads and searches in backend/src/main/java/com/khaleo/flashcard/service/persistence/DeckCardAccessGuard.java
- [X] T036 [US2] Add card operation and search observability events in backend/src/main/java/com/khaleo/flashcard/config/observability/NewRelicDeckMediaInstrumentation.java

**Checkpoint**: User Story 2 is independently functional and testable.

---

## Phase 5: User Story 3 - Attach Media Through Direct Upload Authorization (Priority: P3)

**Goal**: Deliver validated presigned upload authorization flow and reference-aware media lifecycle behavior.

**Independent Test**: Request presigned URL with valid/invalid metadata and rate-limit conditions, upload via URL, and verify media references persist and cleanup only occurs when unreferenced.

### Tests for User Story 3

- [X] T037 [P] [US3] Add contract tests for GET `/api/v1/media/presigned-url` in backend/src/test/java/com/khaleo/flashcard/contract/deckmedia/MediaAuthorizationContractTest.java
- [X] T038 [P] [US3] Add integration tests for media authorization validation and 5-minute expiry behavior in backend/src/test/java/com/khaleo/flashcard/integration/deckmedia/MediaAuthorizationValidationIT.java
- [X] T039 [P] [US3] Add integration tests for per-user rate limiting and 429 responses in backend/src/test/java/com/khaleo/flashcard/integration/deckmedia/MediaAuthorizationRateLimitIT.java
- [X] T040 [P] [US3] Add integration tests for reference-aware media deletion across deck/card updates and deletes in backend/src/test/java/com/khaleo/flashcard/integration/deckmedia/MediaReferenceLifecycleIT.java
- [X] T041 [P] [US3] Add unit tests for media file type/extension/size validation and object key generation in backend/src/test/java/com/khaleo/flashcard/unit/deckmedia/MediaValidationServiceTest.java

### Implementation for User Story 3

- [X] T042 [P] [US3] Add S3 client and presigner configuration in backend/src/main/java/com/khaleo/flashcard/config/media/S3MediaClientConfig.java
- [X] T043 [P] [US3] Create media authorization DTOs in backend/src/main/java/com/khaleo/flashcard/controller/media/dto/MediaAuthorizationResponse.java and backend/src/main/java/com/khaleo/flashcard/controller/media/dto/MediaAuthorizationErrorResponse.java
- [X] T044 [US3] Implement presigned URL generation and metadata validation service in backend/src/main/java/com/khaleo/flashcard/service/media/S3PresignedUrlService.java
- [X] T045 [US3] Implement media authorization orchestration service in backend/src/main/java/com/khaleo/flashcard/service/media/MediaAuthorizationService.java
- [X] T046 [US3] Implement media authorization endpoint in backend/src/main/java/com/khaleo/flashcard/controller/media/MediaController.java
- [X] T047 [US3] Integrate media reference increment/decrement operations into deck/card create-update-delete flows in backend/src/main/java/com/khaleo/flashcard/service/persistence/RelationalPersistenceService.java
- [X] T048 [US3] Implement storage delete-on-zero-reference behavior in backend/src/main/java/com/khaleo/flashcard/service/media/MediaReferenceService.java
- [X] T049 [US3] Add media authorization and cleanup observability events in backend/src/main/java/com/khaleo/flashcard/config/observability/NewRelicDeckMediaInstrumentation.java

**Checkpoint**: User Story 3 is independently functional and testable.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Finalize compliance evidence, infrastructure observability, and regression validation.

- [X] T050 [P] Update feature documentation consistency in specs/003-deck-card-s3-media/research.md, specs/003-deck-card-s3-media/data-model.md, and specs/003-deck-card-s3-media/quickstart.md
- [X] T051 [P] Finalize and verify endpoint/error contracts in specs/003-deck-card-s3-media/contracts/deck-card-media-contract.md
- [X] T052 Add or update CloudWatch alarm definitions for deck/media failure and rate-limit metrics in infra/terraform/cloudwatch-persistence-alarms.tf and infra/terraform/cloudwatch-auth-security-alarms.tf
- [X] T053 Run Terraform formatting and validation for updated infra in infra/terraform/main.tf and infra/terraform/variables.tf
- [X] T054 Execute backend test suite and capture report evidence in backend/build/reports/tests/phase-deck-card-media-summary.md
- [X] T055 Run quickstart verification commands and record final notes in specs/003-deck-card-s3-media/quickstart.md
- [X] T056 Update checklist validation status in specs/003-deck-card-s3-media/checklists/requirements.md

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Setup)**: No dependencies.
- **Phase 2 (Foundational)**: Depends on Phase 1 and blocks all user stories.
- **Phase 3 (US1)**: Depends on Phase 2; delivers MVP deck management.
- **Phase 4 (US2)**: Depends on Phase 2; can proceed in parallel with US1 after foundational completion, but integration with live deck endpoints is simplest after US1 controller contract stabilizes.
- **Phase 5 (US3)**: Depends on Phase 2 and uses deck/card mutation paths from US1/US2 for end-to-end attachment and reference lifecycle validation.
- **Phase 6 (Polish)**: Depends on completion of desired user stories.

### User Story Dependencies

- **US1 (P1)**: Starts after foundational phase; no dependency on other stories.
- **US2 (P2)**: Starts after foundational phase; independent business value but operationally integrates with deck APIs from US1.
- **US3 (P3)**: Starts after foundational phase; independent media authorization path, with full acceptance validation requiring deck/card mutation endpoints.

### Within Each User Story

- Tests should be authored first and verified failing before implementation.
- DTO contracts before service wiring.
- Service logic before controller endpoint exposure.
- Observability and error semantics before story checkpoint.

---

## Parallel Execution Examples

### User Story 1

```bash
Task: "T017 [US1] Add contract tests for deck endpoints in backend/src/test/java/com/khaleo/flashcard/contract/deckmedia/DeckManagementContractTest.java"
Task: "T018 [US1] Add integration tests for deck authorization in backend/src/test/java/com/khaleo/flashcard/integration/deckmedia/DeckManagementAuthorizationIT.java"
Task: "T021 [US1] Create deck DTOs in backend/src/main/java/com/khaleo/flashcard/controller/deck/dto/*.java"
```

### User Story 2

```bash
Task: "T027 [US2] Add contract tests for card endpoints in backend/src/test/java/com/khaleo/flashcard/contract/deckmedia/CardManagementSearchContractTest.java"
Task: "T029 [US2] Add integration tests for search semantics in backend/src/test/java/com/khaleo/flashcard/integration/deckmedia/CardSearchSemanticsIT.java"
Task: "T031 [US2] Create card DTOs in backend/src/main/java/com/khaleo/flashcard/controller/card/dto/*.java"
```

### User Story 3

```bash
Task: "T037 [US3] Add media authorization contract test in backend/src/test/java/com/khaleo/flashcard/contract/deckmedia/MediaAuthorizationContractTest.java"
Task: "T039 [US3] Add rate-limit integration test in backend/src/test/java/com/khaleo/flashcard/integration/deckmedia/MediaAuthorizationRateLimitIT.java"
Task: "T042 [US3] Add S3 presigner configuration in backend/src/main/java/com/khaleo/flashcard/config/media/S3MediaClientConfig.java"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1 and Phase 2.
2. Complete Phase 3 (US1) only.
3. Validate deck CRUD, pagination, and authorization behavior independently.
4. Demo/deploy MVP deck management slice.

### Incremental Delivery

1. Deliver US1 for secure deck management.
2. Deliver US2 for card CRUD and search.
3. Deliver US3 for media upload authorization and reference lifecycle.
4. Complete Phase 6 for compliance, observability, and regression evidence.

### Parallel Team Strategy

1. Team completes Setup + Foundational together.
2. After Phase 2 completion:
   - Engineer A: US1 deck endpoints and auth checks.
   - Engineer B: US2 card CRUD/search behavior.
   - Engineer C: US3 media authorization and lifecycle.
3. Merge and run cross-story regression and observability validation in Phase 6.

---

## Notes

- Task format strictly follows: `- [ ] T### [P] [US#] Description with file path`.
- `[P]` is used only for tasks that can execute in parallel on independent files.
- All stories include explicit tests because this feature changes persistence, authorization, security, runtime integrations, and observability behavior.
- Infrastructure and monitoring tasks are included to satisfy constitutional Terraform and alarm requirements.
