# Tasks: Core Database and Entity Foundation

**Input**: Design documents from `/specs/001-core-database-entities/`
**Prerequisites**: plan.md (required), spec.md (required), research.md, data-model.md, contracts/persistence-contract.md, quickstart.md

**Tests**: Include unit, integration, and contract tests to satisfy constitutional quality gates for persistence, concurrency, and reliability behavior.

**Organization**: Tasks are grouped by user story so each story can be implemented and validated independently.

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Initialize repository structure and baseline backend/IaC files for this feature.

- [X] T001 Create backend module structure in backend/src/main/java/com/khaleo/flashcard/ and backend/src/test/java/com/khaleo/flashcard/
- [X] T002 Create Maven Spring Boot project descriptor in backend/pom.xml
- [X] T003 [P] Create Spring Boot entrypoint class in backend/src/main/java/com/khaleo/flashcard/FlashcardApplication.java
- [X] T004 [P] Create baseline application configuration in backend/src/main/resources/application.yml
- [X] T005 [P] Create Terraform module skeleton for persistence resources in infra/terraform/main.tf
- [X] T006 [P] Create Terraform variables for Aurora and DynamoDB persistence resources in infra/terraform/variables.tf

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Build shared persistence and observability foundation required before user-story implementation.

**CRITICAL**: No user story work starts before this phase is complete.

- [X] T007 Configure Flyway datasource and migration settings in backend/src/main/resources/application.yml
- [X] T008 [P] Create JPA auditing configuration in backend/src/main/java/com/khaleo/flashcard/config/JpaAuditingConfig.java
- [X] T009 [P] Create base auditable mapped superclass in backend/src/main/java/com/khaleo/flashcard/entity/BaseAuditableEntity.java
- [X] T010 [P] Create shared persistence enums in backend/src/main/java/com/khaleo/flashcard/entity/enums/
- [X] T011 Create initial Flyway migration placeholder in backend/src/main/resources/db/migration/V1__init_schema.sql
- [X] T012 [P] Add structured JSON logging baseline for persistence flows in backend/src/main/resources/logback-spring.xml
- [X] T013 [P] Create retry/dead-letter publishing abstraction for async logs in backend/src/main/java/com/khaleo/flashcard/config/activitylog/ActivityLogPublishPolicy.java
- [X] T014 Create integration test container base setup in backend/src/test/java/com/khaleo/flashcard/integration/support/IntegrationPersistenceTestBase.java

**Checkpoint**: Foundation ready for user-story work.

---

## Phase 3: User Story 1 - Persist Learning Domain Data (Priority: P1) 🎯 MVP

**Goal**: Persist User, Deck, Card, and CardLearningState with correct relationships and recoverability after restart.

**Independent Test**: Create user/deck/card/learning-state records, restart app context, and verify records and links remain valid.

### Tests for User Story 1

- [X] T015 [P] [US1] Add migration contract test for relational schema objects in backend/src/test/java/com/khaleo/flashcard/contract/FlywaySchemaContractTest.java
- [X] T016 [P] [US1] Add integration test for user-deck-card persistence flow in backend/src/test/java/com/khaleo/flashcard/integration/UserDeckCardPersistenceIT.java
- [X] T017 [P] [US1] Add integration test for unique active learning state per user-card in backend/src/test/java/com/khaleo/flashcard/integration/CardLearningStateUniquenessIT.java
- [X] T018 [P] [US1] Add unit tests for SM-2 entity default values in backend/src/test/java/com/khaleo/flashcard/unit/entity/CardLearningStateDefaultsTest.java

### Implementation for User Story 1

- [X] T019 [P] [US1] Implement User entity in backend/src/main/java/com/khaleo/flashcard/entity/User.java
- [X] T020 [P] [US1] Implement Deck entity in backend/src/main/java/com/khaleo/flashcard/entity/Deck.java
- [X] T021 [P] [US1] Implement Card entity in backend/src/main/java/com/khaleo/flashcard/entity/Card.java
- [X] T022 [P] [US1] Implement CardLearningState entity with optimistic version field in backend/src/main/java/com/khaleo/flashcard/entity/CardLearningState.java
- [X] T023 [P] [US1] Implement Spring Data repositories for relational entities in backend/src/main/java/com/khaleo/flashcard/repository/
- [X] T024 [US1] Implement V1 Flyway DDL for User, Deck, Card, CardLearningState and constraints in backend/src/main/resources/db/migration/V1__init_schema.sql
- [X] T025 [US1] Implement relational persistence service for create/update workflows in backend/src/main/java/com/khaleo/flashcard/service/persistence/RelationalPersistenceService.java
- [X] T026 [US1] Add persistence failure/success structured logs for relational commits in backend/src/main/java/com/khaleo/flashcard/service/persistence/RelationalPersistenceService.java

**Checkpoint**: User Story 1 is independently functional and testable.

---

## Phase 4: User Story 2 - Capture Study Activity History (Priority: P2)

**Goal**: Store immutable StudyActivityLog events in DynamoDB asynchronously without blocking authoritative Aurora commits.

**Independent Test**: Publish multiple review activity events for one user and verify user-indexed retrieval and immutable event records.

### Tests for User Story 2

- [ ] T027 [P] [US2] Add contract test for StudyActivityLog record shape in backend/src/test/java/com/khaleo/flashcard/contract/StudyActivityLogContractTest.java
- [ ] T028 [P] [US2] Add integration test for async activity log publish success path in backend/src/test/java/com/khaleo/flashcard/integration/ActivityLogPublishIT.java
- [ ] T029 [P] [US2] Add integration test for retry and dead-letter on DynamoDB failure in backend/src/test/java/com/khaleo/flashcard/integration/ActivityLogRetryDeadLetterIT.java
- [ ] T030 [P] [US2] Add unit test for activity log serialization and rating validation in backend/src/test/java/com/khaleo/flashcard/unit/activitylog/StudyActivityLogValidationTest.java

### Implementation for User Story 2

- [ ] T031 [P] [US2] Implement StudyActivityLog DynamoDB model in backend/src/main/java/com/khaleo/flashcard/model/dynamo/StudyActivityLog.java
- [ ] T032 [P] [US2] Implement DynamoDB table mapper/repository in backend/src/main/java/com/khaleo/flashcard/repository/dynamo/StudyActivityLogRepository.java
- [ ] T033 [US2] Implement async activity log publisher service in backend/src/main/java/com/khaleo/flashcard/service/activitylog/StudyActivityLogPublisher.java
- [ ] T034 [US2] Implement retry and dead-letter handling policy in backend/src/main/java/com/khaleo/flashcard/service/activitylog/ActivityLogRetryService.java
- [ ] T035 [US2] Integrate relational commit callback to async log publisher in backend/src/main/java/com/khaleo/flashcard/service/persistence/RelationalPersistenceService.java
- [ ] T036 [US2] Define DynamoDB table and userId index for StudyActivityLog in infra/terraform/dynamodb-study-activity.tf
- [ ] T037 [US2] Add structured logging for async publish, retry, and dead-letter outcomes in backend/src/main/java/com/khaleo/flashcard/service/activitylog/StudyActivityLogPublisher.java

**Checkpoint**: User Story 2 is independently functional and testable.

---

## Phase 5: User Story 3 - Enforce Data Integrity Rules (Priority: P3)

**Goal**: Reject invalid persistence writes and guarantee deterministic constraints for data correctness.

**Independent Test**: Attempt invalid writes (duplicate email, invalid card sides, invalid limits, invalid relationships, concurrency conflicts) and verify deterministic rejection.

### Tests for User Story 3

- [ ] T038 [P] [US3] Add integration test for duplicate email rejection in backend/src/test/java/com/khaleo/flashcard/integration/UserEmailUniquenessIT.java
- [ ] T039 [P] [US3] Add integration test for card-side content validation in backend/src/test/java/com/khaleo/flashcard/integration/CardContentValidationIT.java
- [ ] T040 [P] [US3] Add integration test for dailyLearningLimit range validation in backend/src/test/java/com/khaleo/flashcard/integration/UserDailyLimitValidationIT.java
- [ ] T041 [P] [US3] Add integration test for optimistic lock conflict with one bounded retry in backend/src/test/java/com/khaleo/flashcard/integration/CardLearningStateConcurrencyIT.java

### Implementation for User Story 3

- [ ] T042 [P] [US3] Implement User validation rules for email and dailyLearningLimit in backend/src/main/java/com/khaleo/flashcard/entity/User.java
- [ ] T043 [P] [US3] Implement Card front/back content validation rules in backend/src/main/java/com/khaleo/flashcard/entity/Card.java
- [ ] T044 [US3] Implement service-layer guard for missing relationships and orphan prevention in backend/src/main/java/com/khaleo/flashcard/service/persistence/RelationalPersistenceService.java
- [ ] T045 [US3] Implement optimistic-concurrency retry policy for CardLearningState updates in backend/src/main/java/com/khaleo/flashcard/service/persistence/CardLearningStateUpdateService.java
- [ ] T046 [US3] Add database-level unique and check constraints refinement in backend/src/main/resources/db/migration/V1__init_schema.sql
- [ ] T047 [US3] Add deterministic error mapping for validation failures in backend/src/main/java/com/khaleo/flashcard/service/persistence/PersistenceValidationExceptionMapper.java
- [ ] T048 [US3] Add structured logging for validation and concurrency rejection paths in backend/src/main/java/com/khaleo/flashcard/service/persistence/PersistenceValidationExceptionMapper.java
- [ ] T049 [US3] Update persistence contract with finalized integrity constraints in specs/001-core-database-entities/contracts/persistence-contract.md

**Checkpoint**: User Story 3 is independently functional and testable.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Finalize documentation, observability wiring, and quality gates across all stories.

- [ ] T050 [P] Document migration and persistence architecture decisions in specs/001-core-database-entities/research.md
- [ ] T051 [P] Finalize entity relationship and constraint documentation in specs/001-core-database-entities/data-model.md
- [ ] T052 Add New Relic instrumentation hooks for persistence and async logging paths in backend/src/main/java/com/khaleo/flashcard/config/observability/NewRelicPersistenceInstrumentation.java
- [ ] T053 Add CloudWatch alarm Terraform definitions for persistence error-rate and retry/dead-letter failures in infra/terraform/cloudwatch-persistence-alarms.tf
- [ ] T054 Execute full backend test suite and record coverage output in backend/build/reports/tests/
- [ ] T055 Run quickstart verification steps and update commands if needed in specs/001-core-database-entities/quickstart.md
- [ ] T056 Final compliance review and gate checklist update in specs/001-core-database-entities/checklists/requirements.md

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Setup)**: No dependencies.
- **Phase 2 (Foundational)**: Depends on Phase 1 and blocks all user stories.
- **Phase 3 (US1)**: Depends on Phase 2; delivers MVP persistence foundation.
- **Phase 4 (US2)**: Depends on Phase 2; integrates with US1 persistence service but remains independently testable with seeded IDs.
- **Phase 5 (US3)**: Depends on Phase 3 because integrity enforcement targets US1 entities and update flows.
- **Phase 6 (Polish)**: Depends on completion of selected user stories.

### User Story Dependencies

- **US1 (P1)**: Starts after foundational phase; no dependency on other stories.
- **US2 (P2)**: Starts after foundational phase; optional integration point with US1 commit service.
- **US3 (P3)**: Starts after US1 implementation because it hardens US1 persistence behavior.

### Within Each User Story

- Tests first and failing before implementation.
- Entities/models before services.
- Services before integration wiring.
- Logging and compliance updates before story checkpoint.

---

## Parallel Execution Examples

### User Story 1

```bash
Task: "T019 Implement User entity in backend/src/main/java/com/khaleo/flashcard/entity/User.java"
Task: "T020 Implement Deck entity in backend/src/main/java/com/khaleo/flashcard/entity/Deck.java"
Task: "T021 Implement Card entity in backend/src/main/java/com/khaleo/flashcard/entity/Card.java"
Task: "T022 Implement CardLearningState entity with optimistic version field in backend/src/main/java/com/khaleo/flashcard/entity/CardLearningState.java"
```

### User Story 2

```bash
Task: "T027 Add contract test for StudyActivityLog record shape in backend/src/test/java/com/khaleo/flashcard/contract/StudyActivityLogContractTest.java"
Task: "T028 Add integration test for async activity log publish success path in backend/src/test/java/com/khaleo/flashcard/integration/ActivityLogPublishIT.java"
Task: "T031 Implement StudyActivityLog DynamoDB model in backend/src/main/java/com/khaleo/flashcard/model/dynamo/StudyActivityLog.java"
Task: "T032 Implement DynamoDB table mapper/repository in backend/src/main/java/com/khaleo/flashcard/repository/dynamo/StudyActivityLogRepository.java"
```

### User Story 3

```bash
Task: "T038 Add integration test for duplicate email rejection in backend/src/test/java/com/khaleo/flashcard/integration/UserEmailUniquenessIT.java"
Task: "T039 Add integration test for card-side content validation in backend/src/test/java/com/khaleo/flashcard/integration/CardContentValidationIT.java"
Task: "T042 Implement User validation rules for email and dailyLearningLimit in backend/src/main/java/com/khaleo/flashcard/entity/User.java"
Task: "T043 Implement Card front/back content validation rules in backend/src/main/java/com/khaleo/flashcard/entity/Card.java"
```

---

## Implementation Strategy

### MVP First (US1)

1. Complete Setup and Foundational phases.
2. Deliver US1 relational schema/entities/services.
3. Validate US1 independent test before adding async logging or hardening.

### Incremental Delivery

1. US1 establishes authoritative transactional persistence.
2. US2 adds asynchronous activity logging with retry/dead-letter reliability.
3. US3 adds strict validation and concurrency hardening.
4. Polish phase finalizes observability and compliance evidence.

### Parallel Team Strategy

1. Team completes Phase 1-2 together.
2. One engineer focuses on US1 core entities/migrations.
3. One engineer focuses on US2 async DynamoDB pipeline.
4. One engineer focuses on US3 validation/concurrency test hardening after US1 base merges.

---

## Notes

- Task format follows strict checklist structure: `- [ ] T### [P] [US#] Description with file path`.
- `[P]` markers are only used where tasks touch separate files with no incomplete dependency.
- All user stories include independent tests and story checkpoints.
- Pagination is intentionally not implemented in this feature because no list API endpoint is in scope.
