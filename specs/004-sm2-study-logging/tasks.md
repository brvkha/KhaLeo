# Tasks: FSRS v6 Spaced Repetition and Study Activity Logging

**Input**: Design documents from `/specs/004-sm2-study-logging/`
**Prerequisites**: plan.md (required), spec.md (required), research.md, data-model.md, contracts/study-engine-contract.md, quickstart.md

**Tests**: The constitution requires automated tests for behavior, persistence, scheduling, observability, and runtime integrations. This task list includes explicit contract, integration, and unit test tasks.

**Organization**: Tasks are grouped by user story for independent implementation, validation, and delivery.

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Prepare feature modules, DTO contracts, and DynamoDB config baseline for study workflows.

- [X] T001 Create study package structure in backend/src/main/java/com/khaleo/flashcard/controller/study/, backend/src/main/java/com/khaleo/flashcard/service/study/, and backend/src/main/java/com/khaleo/flashcard/model/study/
- [X] T002 [P] Add study endpoint and async logging configuration keys in backend/src/main/resources/application.yml
- [X] T003 [P] Add/update DynamoDB table and metric variables for study logging in infra/terraform/variables.tf and infra/terraform/dynamodb-study-activity.tf
- [X] T004 [P] Scaffold study test package structure in backend/src/test/java/com/khaleo/flashcard/contract/study/, backend/src/test/java/com/khaleo/flashcard/integration/study/, and backend/src/test/java/com/khaleo/flashcard/unit/study/

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Implement shared domain and persistence primitives required by all user stories.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

- [X] T005 Add FSRS scheduling fields (stability, difficulty, elapsed/scheduled days, reps, lapses) in backend/src/main/java/com/khaleo/flashcard/entity/CardLearningState.java
- [X] T006 [P] Add query methods for due learning/review tiers and quota support in backend/src/main/java/com/khaleo/flashcard/repository/CardLearningStateRepository.java
- [X] T007 [P] Add pagination token model and response envelope types in backend/src/main/java/com/khaleo/flashcard/model/study/StudyPaginationToken.java and backend/src/main/java/com/khaleo/flashcard/model/study/NextCardsPageResponse.java
- [X] T008 Implement shared authorization and deck/card access guard for study endpoints in backend/src/main/java/com/khaleo/flashcard/service/study/StudyAccessService.java
- [X] T009 [P] Add study-domain error codes and exception mappings in backend/src/main/java/com/khaleo/flashcard/model/error/ErrorCode.java and backend/src/main/java/com/khaleo/flashcard/controller/GlobalExceptionHandler.java
- [X] T010 [P] Add observability event hooks for study retrieval/rating/logging in backend/src/main/java/com/khaleo/flashcard/config/observability/NewRelicDeckMediaInstrumentation.java

**Checkpoint**: Foundation ready - user story implementation can now begin.

---

## Phase 3: User Story 1 - Study Due Cards Reliably (Priority: P1) 🎯 MVP

**Goal**: Provide authenticated learners with correctly ordered due cards and new-card gating by daily unique-card quota.

**Independent Test**: Request next cards for a deck with mixed states and verify ordering, pagination, and daily limit behavior end-to-end.

### Tests for User Story 1

- [X] T011 [P] [US1] Add next-cards API contract tests in backend/src/test/java/com/khaleo/flashcard/contract/study/StudyNextCardsContractTest.java
- [X] T012 [P] [US1] Add due-ordering and quota integration tests in backend/src/test/java/com/khaleo/flashcard/integration/study/StudyNextCardsOrderingIT.java
- [X] T013 [P] [US1] Add pagination-token unit tests in backend/src/test/java/com/khaleo/flashcard/unit/study/StudyPaginationTokenCodecTest.java

### Implementation for User Story 1

- [X] T014 [P] [US1] Add next-cards request/response DTOs in backend/src/main/java/com/khaleo/flashcard/model/study/NextCardsRequest.java and backend/src/main/java/com/khaleo/flashcard/model/study/StudyCardSummary.java
- [X] T015 [P] [US1] Implement daily unique-card quota computation service in backend/src/main/java/com/khaleo/flashcard/service/study/StudyDailyQuotaService.java
- [X] T016 [US1] Implement due-card selection orchestration with tier ordering in backend/src/main/java/com/khaleo/flashcard/service/study/NextCardsService.java
- [X] T017 [US1] Implement GET /api/v1/study-session/decks/{deckId}/next-cards in backend/src/main/java/com/khaleo/flashcard/controller/study/StudySessionController.java
- [X] T018 [US1] Wire pagination validation and continuation token handling in backend/src/main/java/com/khaleo/flashcard/service/study/NextCardsService.java and backend/src/main/java/com/khaleo/flashcard/model/study/NextCardsPageResponse.java
- [X] T019 [US1] Emit retrieval success/denial/failure telemetry in backend/src/main/java/com/khaleo/flashcard/service/study/NextCardsService.java

**Checkpoint**: User Story 1 is independently functional and testable.

---

## Phase 4: User Story 2 - Rate Cards With Correct Scheduling (Priority: P2)

**Goal**: Apply validated learner ratings to card state, scheduled days, difficulty, and stability with strict FSRS v6 fidelity.

**Independent Test**: Submit ratings across NEW/LEARNING/REVIEW/RELEARNING paths and verify deterministic transitions and FSRS formula compliance.

### Tests for User Story 2

- [X] T020 [P] [US2] Add rating API contract tests in backend/src/test/java/com/khaleo/flashcard/contract/study/StudyRateCardContractTest.java
- [X] T021 [P] [US2] Add rating-transition integration tests in backend/src/test/java/com/khaleo/flashcard/integration/study/StudyRateCardTransitionIT.java
- [X] T022 [P] [US2] Add FSRS formula unit tests in backend/src/test/java/com/khaleo/flashcard/unit/study/SpacedRepetitionServiceTest.java

### Implementation for User Story 2

- [X] T023 [P] [US2] Add rating request/response DTOs in backend/src/main/java/com/khaleo/flashcard/model/study/RateCardRequest.java and backend/src/main/java/com/khaleo/flashcard/model/study/RateCardResponse.java
- [X] T024 [P] [US2] Implement FSRS scheduling math and D/S/R update formulas in backend/src/main/java/com/khaleo/flashcard/service/study/SpacedRepetitionService.java
- [X] T025 [US2] Implement rating persistence orchestration in backend/src/main/java/com/khaleo/flashcard/service/study/StudyRatingService.java
- [X] T026 [US2] Implement POST /api/v1/study-session/cards/{cardId}/rate in backend/src/main/java/com/khaleo/flashcard/controller/study/StudySessionController.java
- [X] T027 [US2] Add request validation and deterministic error mapping for rating/time inputs in backend/src/main/java/com/khaleo/flashcard/controller/study/StudySessionController.java and backend/src/main/java/com/khaleo/flashcard/controller/GlobalExceptionHandler.java
- [X] T028 [US2] Emit rating transition telemetry and compliance markers in backend/src/main/java/com/khaleo/flashcard/service/study/StudyRatingService.java

**Checkpoint**: User Stories 1 and 2 are independently testable.

---

## Phase 5: User Story 3 - Record Study Activity Without Slowing Study (Priority: P3)

**Goal**: Persist rating activity to DynamoDB asynchronously while preserving fast rating API responses.

**Independent Test**: Verify successful rating responses are immediate and async log records are written (or failures are observable without rolling back scheduling updates).

### Tests for User Story 3

- [X] T029 [P] [US3] Add async logging contract coverage for rating responses in backend/src/test/java/com/khaleo/flashcard/contract/study/StudyAsyncLoggingContractTest.java
- [X] T030 [P] [US3] Add async logging resilience integration tests in backend/src/test/java/com/khaleo/flashcard/integration/study/StudyActivityLoggingResilienceIT.java
- [X] T031 [P] [US3] Add unit tests for async publish and failure handling in backend/src/test/java/com/khaleo/flashcard/unit/study/StudyActivityLogPublisherTest.java

### Implementation for User Story 3

- [X] T032 [P] [US3] Add/extend DynamoDB study log item mapping in backend/src/main/java/com/khaleo/flashcard/model/dynamo/StudyActivityLog.java
- [X] T033 [P] [US3] Implement async activity logging service in backend/src/main/java/com/khaleo/flashcard/service/activitylog/StudyActivityLogPublisher.java
- [X] T034 [US3] Integrate non-blocking log publish into rating workflow in backend/src/main/java/com/khaleo/flashcard/service/study/StudyRatingService.java
- [X] T035 [US3] Implement logging failure capture and retry handoff in backend/src/main/java/com/khaleo/flashcard/service/activitylog/ActivityLogRetryService.java
- [X] T036 [US3] Add async logging telemetry in backend/src/main/java/com/khaleo/flashcard/service/activitylog/StudyActivityLogPublisher.java and backend/src/main/java/com/khaleo/flashcard/config/observability/NewRelicDeckMediaInstrumentation.java

**Checkpoint**: All user stories are independently functional and testable.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Complete cross-story validation, observability hardening, and delivery evidence.

- [X] T037 [P] Add CloudWatch alarms for study retrieval/rating/logging failures in infra/terraform/cloudwatch-persistence-alarms.tf and infra/terraform/cloudwatch-auth-security-alarms.tf
- [X] T038 [P] Add/refresh study feature documentation in KhaLeoDocs/sm2_engine_and_dynamodb_logging.md (legacy filename, FSRS content)
- [X] T039 Run full backend validation suite and capture summary in backend/build/reports/tests/phase-sm2-study-logging-summary.md
- [X] T040 Validate quickstart flow and record verification evidence in specs/004-sm2-study-logging/quickstart.md
- [X] T041 Final compliance and artifact alignment pass across specs/004-sm2-study-logging/spec.md, specs/004-sm2-study-logging/plan.md, and specs/004-sm2-study-logging/tasks.md

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Setup)**: No dependencies.
- **Phase 2 (Foundational)**: Depends on Phase 1 and blocks all user story work.
- **Phase 3 (US1)**: Depends on Phase 2.
- **Phase 4 (US2)**: Depends on Phase 2 and can run in parallel with US1 if staffed.
- **Phase 5 (US3)**: Depends on Phase 2 and can run in parallel with US1/US2 if staffed.
- **Phase 6 (Polish)**: Depends on completion of selected user stories.

### User Story Dependencies

- **US1 (P1)**: No dependency on US2/US3 once foundational tasks are complete.
- **US2 (P2)**: Uses shared foundational services; independent from US1 features except shared endpoint/controller file coordination.
- **US3 (P3)**: Depends on rating workflow from US2 for integration point but remains independently testable as an additive concern.

### Within Each User Story

- Write tests first and confirm they fail before implementation.
- Implement DTO/model and service logic before controller wiring.
- Complete observability and error mapping before closing the story.

## Parallel Opportunities

- Setup tasks T002, T003, and T004 can run in parallel.
- Foundational tasks T006, T007, T009, and T010 can run in parallel.
- US1 test tasks T011-T013 can run in parallel.
- US2 test tasks T020-T022 can run in parallel.
- US3 test tasks T029-T031 can run in parallel.
- US3 implementation tasks T032 and T033 can run in parallel.
- Phase 6 documentation/alarm tasks T037 and T038 can run in parallel.

## Parallel Example: User Story 1

```bash
# Parallel US1 test execution
Task: "T011 Contract test for GET /next-cards"
Task: "T012 Integration test for tier ordering and quota"
Task: "T013 Unit test for pagination token codec"

# Parallel US1 model/service preparation
Task: "T014 Add next-cards DTOs"
Task: "T015 Implement daily quota service"
```

## Parallel Example: User Story 2

```bash
# Parallel US2 test execution
Task: "T020 Contract test for POST /rate"
Task: "T021 Integration test for state transitions"
Task: "T022 Unit test for FSRS formulas"

# Parallel US2 core build tasks
Task: "T023 Add rating DTOs"
Task: "T024 Implement spaced repetition math"
```

## Parallel Example: User Story 3

```bash
# Parallel US3 test execution
Task: "T029 Contract test for async logging behavior"
Task: "T030 Integration test for logging resilience"
Task: "T031 Unit test for async publisher"

# Parallel US3 implementation prep
Task: "T032 Extend DynamoDB log item mapping"
Task: "T033 Implement async log publisher"
```

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1 (Setup).
2. Complete Phase 2 (Foundational).
3. Complete Phase 3 (US1).
4. Validate US1 independently before adding more scope.

### Incremental Delivery

1. Foundation first: Setup + Foundational.
2. Deliver US1 (MVP), validate, and demo.
3. Deliver US2, validate scheduling fidelity, and demo.
4. Deliver US3, validate async logging resilience, and demo.
5. Finish with Phase 6 cross-cutting validation.

### Parallel Team Strategy

1. Team completes Phase 1 and Phase 2 together.
2. After checkpoint, parallelize stories:
   - Engineer A: US1 tasks.
   - Engineer B: US2 tasks.
   - Engineer C: US3 tasks.
3. Merge and execute Phase 6 regression and observability validation.

## Notes

- [P] tasks touch separate files or have no dependency on unfinished tasks.
- [USx] labels provide story traceability and independent-delivery alignment.
- All tasks include explicit file paths to be immediately executable by an implementation agent.
