# Tasks: Administration, Observability, and DevOps Pipeline

**Input**: Design documents from `/specs/005-admin-observability-devops/`
**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/

**Tests**: The constitution requires automated tests sufficient to preserve the 80% overall coverage target and the 60/30/10 unit/integration/E2E testing pyramid. Include explicit test tasks whenever the feature changes behavior, persistence, security, scheduling, infrastructure, or runtime integrations.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Initialize feature scaffolding and dependency baselines for admin, observability, and deployment work.

- [X] T001 Create admin package scaffolding in backend/src/main/java/com/khaleo/flashcard/controller/admin and backend/src/main/java/com/khaleo/flashcard/service/admin
- [X] T002 Create admin test package scaffolding in backend/src/test/java/com/khaleo/flashcard/contract/admin and backend/src/test/java/com/khaleo/flashcard/integration/admin
- [X] T003 [P] Add Splunk logging dependency placeholders in backend/pom.xml
- [X] T004 [P] Create deployment workflow skeleton in .github/workflows/deploy-backend.yml

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core security, domain, and observability prerequisites that block all user stories.

**CRITICAL**: No user story work can begin until this phase is complete.

- [X] T005 Add banned-user state fields and constraints to backend/src/main/java/com/khaleo/flashcard/entity/User.java
- [X] T006 Add Flyway migration for banned-user columns in backend/src/main/resources/db/migration/V5__admin_moderation_schema.sql
- [X] T007 [P] Add admin action audit entity in backend/src/main/java/com/khaleo/flashcard/entity/AdminModerationAction.java
- [X] T008 [P] Add admin action repository in backend/src/main/java/com/khaleo/flashcard/repository/AdminModerationActionRepository.java
- [X] T009 Enable method security and admin route policy in backend/src/main/java/com/khaleo/flashcard/config/security/SecurityConfig.java
- [X] T010 Enforce banned-user request rejection in backend/src/main/java/com/khaleo/flashcard/config/security/JwtAuthenticationFilter.java
- [X] T011 [P] Add banned-user error mapping in backend/src/main/java/com/khaleo/flashcard/config/security/AuthExceptionHandler.java and backend/src/main/java/com/khaleo/flashcard/controller/GlobalExceptionHandler.java

**Checkpoint**: Foundation ready. User story implementation can now proceed.

---

## Phase 3: User Story 1 - Moderate Platform Content and Access (Priority: P1)

**Goal**: Deliver admin stats and moderation APIs with immediate banned-user enforcement and auditable outcomes.

**Independent Test**: Authenticate as admin, call `/api/v1/admin/stats`, ban a user, delete a deck, edit a card, and verify non-admin access is denied.

### Tests for User Story 1

- [X] T012 [P] [US1] Add admin contract tests for stats/ban/deck-delete/card-edit endpoints in backend/src/test/java/com/khaleo/flashcard/contract/admin/AdminManagementContractTest.java
- [X] T013 [P] [US1] Add integration test for immediate banned-user request blocking in backend/src/test/java/com/khaleo/flashcard/integration/admin/AdminBanImmediateEnforcementIT.java
- [X] T014 [P] [US1] Add unit tests for admin moderation service logic in backend/src/test/java/com/khaleo/flashcard/unit/admin/AdminModerationServiceTest.java

### Implementation for User Story 1

- [X] T015 [P] [US1] Add admin stats response model in backend/src/main/java/com/khaleo/flashcard/controller/admin/dto/AdminStatsResponse.java
- [X] T016 [P] [US1] Add admin card update request model in backend/src/main/java/com/khaleo/flashcard/controller/admin/dto/AdminCardUpdateRequest.java
- [X] T017 [US1] Implement platform stats aggregation service in backend/src/main/java/com/khaleo/flashcard/service/admin/AdminStatsService.java
- [X] T018 [US1] Implement moderation orchestration service with audit logging in backend/src/main/java/com/khaleo/flashcard/service/admin/AdminModerationService.java
- [X] T019 [US1] Implement admin API controller endpoints in backend/src/main/java/com/khaleo/flashcard/controller/admin/AdminController.java
- [X] T020 [US1] Add role-based method authorization annotations in backend/src/main/java/com/khaleo/flashcard/controller/admin/AdminController.java
- [X] T021 [US1] Add ban-state checks in auth refresh flow in backend/src/main/java/com/khaleo/flashcard/service/auth/TokenRefreshService.java
- [X] T022 [US1] Wire deck/card moderation operations in backend/src/main/java/com/khaleo/flashcard/service/persistence/RelationalPersistenceService.java
- [X] T023 [US1] Emit admin action observability events in backend/src/main/java/com/khaleo/flashcard/config/observability/NewRelicAuthInstrumentation.java

**Checkpoint**: User Story 1 is independently functional and testable.

---

## Phase 4: User Story 2 - Operate with Actionable Observability (Priority: P2)

**Goal**: Provide async Splunk JSON log shipping, New Relic runtime support, and alarmable operational signals for admin/deployment paths.

**Independent Test**: Generate admin/auth/deploy events and verify structured logs, async shipping behavior, and instrumentation/alarm signal emission.

### Tests for User Story 2

- [X] T024 [P] [US2] Add logging configuration contract test for JSON and Splunk appender wiring in backend/src/test/java/com/khaleo/flashcard/contract/observability/LoggingConfigurationContractTest.java
- [X] T025 [P] [US2] Add integration test for non-blocking logging failure behavior in backend/src/test/java/com/khaleo/flashcard/integration/observability/SplunkAsyncNonBlockingIT.java
- [X] T026 [P] [US2] Add unit tests for observability instrumentation events in backend/src/test/java/com/khaleo/flashcard/unit/observability/AdminObservabilityInstrumentationTest.java

### Implementation for User Story 2

- [X] T027 [P] [US2] Add Splunk Java logging dependency in backend/pom.xml
- [X] T028 [US2] Implement async Splunk HEC appender configuration in backend/src/main/resources/logback-spring.xml
- [X] T029 [US2] Add Splunk/New Relic configuration properties in backend/src/main/resources/application.yml
- [X] T030 [US2] Add deployment/admin instrumentation hooks in backend/src/main/java/com/khaleo/flashcard/config/observability/NewRelicPersistenceInstrumentation.java
- [X] T031 [US2] Add admin observability helper for structured event names in backend/src/main/java/com/khaleo/flashcard/config/observability/NewRelicDeckMediaInstrumentation.java
- [X] T032 [US2] Add CloudWatch alarm for admin authorization denials in infra/terraform/cloudwatch-auth-security-alarms.tf
- [X] T033 [US2] Add CloudWatch alarm for deployment command failures in infra/terraform/cloudwatch-persistence-alarms.tf
- [X] T034 [US2] Add new alarm threshold variables in infra/terraform/variables.tf

**Checkpoint**: User Story 2 is independently functional and testable.

---

## Phase 5: User Story 3 - Deploy Backend Changes Safely (Priority: P3)

**Goal**: Deliver immutable commit-SHA artifact deployment via GitHub Actions and SSM with all-target dispatch and fail-on-any-target semantics.

**Independent Test**: Push to main (or workflow dispatch), verify SHA artifact upload, SSM dispatch to all tagged targets, and overall run fails if any target fails.

### Tests for User Story 3

- [X] T035 [P] [US3] Add workflow validation test for deploy-backend semantics in .github/workflows/deploy-backend.yml
- [X] T036 [P] [US3] Add deployment script dry-run test for SSM result aggregation in backend/src/test/java/com/khaleo/flashcard/integration/deploy/DeploymentAggregationIT.java

### Implementation for User Story 3

- [X] T037 [US3] Implement full GitHub Actions deploy workflow in .github/workflows/deploy-backend.yml
- [X] T038 [US3] Add immutable artifact naming and S3 upload logic in .github/workflows/deploy-backend.yml
- [X] T039 [US3] Add SSM dispatch and per-target status polling logic in .github/workflows/deploy-backend.yml
- [X] T040 [US3] Enforce fail-if-any-target-fails logic in .github/workflows/deploy-backend.yml
- [X] T041 [US3] Add EC2 deployment command script for jar update and service restart in backend/scripts/deploy-via-ssm.sh
- [X] T042 [US3] Add Terraform variables for deploy target tags and artifact bucket in infra/terraform/variables.tf
- [X] T043 [US3] Add deployment support outputs in infra/terraform/main.tf

**Checkpoint**: User Story 3 is independently functional and testable.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Final validation, documentation alignment, and quality gate checks across all stories.

- [X] T044 [P] Update admin/observability/deploy runbook notes in KhaLeoDocs/admin_observability_devops.md
- [X] T045 Validate quickstart commands and update evidence notes in specs/005-admin-observability-devops/quickstart.md
- [X] T046 Run full backend verification suite and capture summary in backend/build/reports/tests/phase6-test-and-coverage-summary.md
- [X] T047 [P] Validate Terraform formatting and plan outputs in infra/terraform/main.tf and infra/terraform/variables.tf
- [X] T048 Validate required observability outputs for Splunk, New Relic, and CloudWatch in specs/005-admin-observability-devops/plan.md

---

## Dependencies & Execution Order

### Phase Dependencies

- Setup (Phase 1): No dependencies, can start immediately.
- Foundational (Phase 2): Depends on Setup completion and blocks all user stories.
- User Stories (Phase 3+): Depend on Foundational completion.
- Polish (Phase 6): Depends on completion of all selected user stories.

### User Story Dependencies

- US1 (P1): Starts after Phase 2 and delivers MVP admin operations.
- US2 (P2): Starts after Phase 2 and can proceed in parallel with US1, but should integrate US1 event names for observability coverage.
- US3 (P3): Starts after Phase 2 and can proceed in parallel with US1/US2, but must consume observability/alarm conventions from US2.

### Within Each User Story

- Tests first and failing before implementation for changed behavior and integrations.
- DTO/entity updates before service logic.
- Service logic before controller/workflow orchestration.
- Observability and failure-path validation before story completion.

## Parallel Opportunities

- Phase 1: T003 and T004 are parallel.
- Phase 2: T007, T008, and T011 can run in parallel after T005/T006.
- US1: T012-T014 and T015-T016 are parallelizable.
- US2: T024-T026 and T027 can run in parallel.
- US3: T035-T036 can run in parallel; T042 can run in parallel with T037-T041.
- Phase 6: T044, T047, and T048 can run in parallel.

## Parallel Example: User Story 1

```bash
# Run US1 tests in parallel workstreams
Task: "T012 [US1] backend/src/test/java/com/khaleo/flashcard/contract/admin/AdminManagementContractTest.java"
Task: "T013 [US1] backend/src/test/java/com/khaleo/flashcard/integration/admin/AdminBanImmediateEnforcementIT.java"
Task: "T014 [US1] backend/src/test/java/com/khaleo/flashcard/unit/admin/AdminModerationServiceTest.java"

# Build US1 DTOs in parallel
Task: "T015 [US1] backend/src/main/java/com/khaleo/flashcard/controller/admin/dto/AdminStatsResponse.java"
Task: "T016 [US1] backend/src/main/java/com/khaleo/flashcard/controller/admin/dto/AdminCardUpdateRequest.java"
```

## Implementation Strategy

### MVP First (User Story 1)

1. Complete Phase 1 and Phase 2.
2. Complete Phase 3 (US1).
3. Validate admin API behavior and immediate ban enforcement.
4. Demo/deploy MVP.

### Incremental Delivery

1. Complete Setup + Foundational.
2. Deliver US1 and validate independently.
3. Deliver US2 and validate independently.
4. Deliver US3 and validate independently.
5. Complete Phase 6 cross-cutting validation.

### Parallel Team Strategy

1. Team completes Setup + Foundational together.
2. After foundation is done:
   - Engineer A: US1 (admin APIs and ban enforcement)
   - Engineer B: US2 (observability and alarms)
   - Engineer C: US3 (GitHub Actions + SSM deployment)
3. Merge and execute Phase 6 validation.

## Notes

- [P] tasks indicate no blocking file-level dependency on incomplete tasks.
- [USx] labels map all story-phase tasks to their corresponding user story.
- Every task includes a concrete file path for direct execution by an LLM agent.
- This task list preserves constitutional requirements for security, observability, Terraform governance, and quality gates.
