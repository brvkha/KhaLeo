# Tasks: Enterprise AWS Infrastructure and Full Terraform/GitHub Actions Automation

**Input**: Design documents from `/specs/feature/enterprise-aws-infra-cicd/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/cicd-infra-contract.md

**Tests**: This feature changes infrastructure, security contracts, and deployment behavior. Automated test and validation tasks are included to preserve constitutional quality gates.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing.

## Format: `[ID] [P?] [Story] Description`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Prepare deployment contract scaffolding and baseline CI/CD inputs.

- [X] T001 Create Terraform variable definitions for CI/CD environment contract in infra/terraform/variables.tf
- [X] T002 Add Terraform outputs for deploy contract values in infra/terraform/outputs.tf
- [X] T003 [P] Add repository deployment documentation for enterprise CI/CD in README.md
- [X] T004 [P] Add feature quickstart execution steps for operators in specs/feature/enterprise-aws-infra-cicd/quickstart.md

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Implement shared security and pipeline foundations required before user-story delivery.

**CRITICAL**: No user-story implementation should start before this phase is complete.

- [X] T005 Implement OIDC trust policy and deploy role permissions for GitHub Actions in infra/terraform/main.tf
- [X] T006 Implement Terraform-managed GitHub production environment contract (secrets metadata/variables) in infra/terraform/main.tf
- [X] T007 [P] Add Terraform validation and plan checks to CI workflow in .github/workflows/ci.yml
- [X] T008 [P] Harden backend deploy script error handling and target diagnostics in backend/scripts/deploy-via-ssm.sh
- [X] T009 Configure backend runtime secret references for production profile in backend/src/main/resources/application.yml
- [X] T010 [P] Add CloudWatch alarm coverage for deployment and backend 5xx signals in infra/terraform/cloudwatch-auth-security-alarms.tf

**Checkpoint**: Foundation complete; user stories can now be implemented independently.

---

## Phase 3: User Story 1 - One-Push Release Automation (Priority: P1) 🎯 MVP

**Goal**: Push to main triggers CI and approval-gated production deployment for immutable backend and frontend artifacts.

**Independent Test**: Push a commit to main, approve production environment, and verify both deploy workflows complete with run summaries and immutable SHA traceability.

### Tests for User Story 1

- [X] T011 [P] [US1] Add CI workflow smoke assertions for backend/frontend stage gating in .github/workflows/ci.yml
- [X] T012 [P] [US1] Add backend deployment integration test for immutable SHA upload and SSM command polling in backend/src/test/java/com/khaleo/flashcard/integration/deploy/BackendDeploymentWorkflowIT.java
- [X] T013 [P] [US1] Add frontend deployment contract test for S3 sync and CloudFront invalidation steps in frontend/tests/e2e/deploy-frontend-contract.spec.ts

### Implementation for User Story 1

- [X] T014 [US1] Implement approval-gated backend deployment workflow with immutable artifact path semantics in .github/workflows/deploy-backend.yml
- [X] T015 [US1] Implement approval-gated frontend deployment workflow with immutable artifact selection support in .github/workflows/deploy-frontend.yml
- [X] T016 [US1] Implement deployment summary outputs (sha, target health, rollback instructions) in .github/workflows/deploy-backend.yml
- [X] T017 [US1] Implement frontend deployment summary output (sha, invalidation id, bucket) in .github/workflows/deploy-frontend.yml
- [X] T018 [US1] Add manual rollback-by-sha workflow_dispatch usage documentation in specs/feature/enterprise-aws-infra-cicd/quickstart.md

**Checkpoint**: US1 is deployable and independently verifiable.

---

## Phase 4: User Story 2 - Domain and Network Reliability (Priority: P1)

**Goal**: Route53, CloudFront, WAF, ALB, and multi-AZ backend topology provide resilient traffic routing.

**Independent Test**: Validate DNS records for root and API domain, healthy target groups across private subnets, and WAF association on ALB.

### Tests for User Story 2

- [X] T019 [P] [US2] Add Terraform validation test for Route53, CloudFront, and ALB wiring in infra/terraform/tests/network_dns_validation.tftest.hcl
- [X] T020 [P] [US2] Add Terraform validation test for backend subnets and target group health-check contract in infra/terraform/tests/backend_topology_validation.tftest.hcl
- [X] T021 [P] [US2] Add deployment smoke test procedure for DNS and health endpoints in specs/feature/enterprise-aws-infra-cicd/quickstart.md

### Implementation for User Story 2

- [X] T022 [US2] Implement Route53 records for khaleoshop.click and api.khaleoshop.click in infra/terraform/main.tf
- [X] T023 [US2] Implement CloudFront distribution and S3 origin bindings for frontend delivery in infra/terraform/main.tf
- [X] T024 [US2] Implement ALB listeners, target groups, and health check configuration in infra/terraform/main.tf
- [X] T025 [US2] Implement WAF web ACL association with backend ALB in infra/terraform/main.tf
- [X] T026 [US2] Implement private multi-AZ backend subnet mapping and scaling-group placement in infra/terraform/main.tf

**Checkpoint**: US2 routing and resilience behaviors are independently testable.

---

## Phase 5: User Story 3 - Secure Runtime Configuration (Priority: P1)

**Goal**: Production runtime and deployment credentials use least privilege with Secrets Manager and OIDC-only access.

**Independent Test**: Verify GitHub assumes the constrained role via OIDC, EC2 runtime can read required secrets, and no static AWS deploy keys are used.

### Tests for User Story 3

- [X] T027 [P] [US3] Add IAM policy contract test for restricted OIDC trust claims in infra/terraform/tests/oidc_trust_validation.tftest.hcl
- [X] T028 [P] [US3] Add runtime secret access integration test for Secrets Manager-backed boot configuration in backend/src/test/java/com/khaleo/flashcard/integration/security/RuntimeSecretAccessIT.java
- [X] T029 [P] [US3] Add workflow guard check that fails when long-lived AWS keys are configured for deployment in .github/workflows/ci.yml

### Implementation for User Story 3

- [X] T030 [US3] Implement least-privilege IAM policies for deploy role and EC2 runtime role in infra/terraform/main.tf
- [X] T031 [US3] Implement Secrets Manager secret reference mapping for DB/JWT/SES in infra/terraform/variables.tf
- [X] T032 [US3] Implement backend runtime environment variable wiring for secret identifiers in backend/src/main/resources/application.yml
- [X] T033 [US3] Implement deploy workflow role assumption constraints for production environment only in .github/workflows/deploy-backend.yml
- [X] T034 [US3] Document production secrets and variables contract for operators in specs/feature/enterprise-aws-infra-cicd/quickstart.md

**Checkpoint**: US3 security controls are independently verifiable.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Finish governance, traceability, and release-readiness work across stories.

- [ ] T035 [P] Validate end-to-end quickstart by executing plan/apply and deployment flow in specs/feature/enterprise-aws-infra-cicd/quickstart.md
- [X] T036 [P] Align contract details with final workflows and Terraform outputs in specs/feature/enterprise-aws-infra-cicd/contracts/cicd-infra-contract.md
- [X] T037 Add final compliance checklist for constitution gates in specs/feature/enterprise-aws-infra-cicd/plan.md
- [X] T038 Record release evidence and rollback runbook references in README.md

---

## Dependencies & Execution Order

### Phase Dependencies

- Setup (Phase 1): Start immediately.
- Foundational (Phase 2): Depends on Setup and blocks all user stories.
- User Story Phases (Phase 3-5): Depend on Foundational completion.
- Polish (Phase 6): Depends on completion of the selected user stories.

### User Story Dependencies

- US1 (P1): Starts after Foundational; no dependency on US2/US3.
- US2 (P1): Starts after Foundational; independent from US1 implementation details.
- US3 (P1): Starts after Foundational; independent from US1/US2 feature logic.

### Within Each User Story

- Write/enable tests first where feasible and ensure they fail before implementation.
- Implement infrastructure/workflow contracts before documentation finalization.
- Complete story-level verification before moving to polish.

## Parallel Opportunities

- Phase 1: T003 and T004 can run in parallel with T001/T002 after folder targets are confirmed.
- Phase 2: T007, T008, and T010 can run in parallel once T005/T006 are underway.
- US1: T011-T013 are parallel; T014 and T015 can execute in parallel; T016/T017 follow each workflow update.
- US2: T019-T021 are parallel; T022-T026 can be split across DNS/CDN/ALB/WAF/subnet implementers.
- US3: T027-T029 are parallel; T030-T032 can proceed in parallel, with T033 after role policies are in place.

---

## Parallel Example: User Story 1

```bash
# Parallel test preparation
Task: T011 [US1] CI gating smoke assertions in .github/workflows/ci.yml
Task: T012 [US1] Backend deployment integration test in backend/src/test/java/com/khaleo/flashcard/integration/deploy/BackendDeploymentWorkflowIT.java
Task: T013 [US1] Frontend deployment contract test in frontend/tests/e2e/deploy-frontend-contract.spec.ts

# Parallel workflow implementation
Task: T014 [US1] Backend deploy workflow in .github/workflows/deploy-backend.yml
Task: T015 [US1] Frontend deploy workflow in .github/workflows/deploy-frontend.yml
```

---

## Implementation Strategy

### MVP First (US1)

1. Complete Phase 1 and Phase 2.
2. Deliver Phase 3 (US1) end-to-end.
3. Validate manual CI plus approval-gated deploy flow.
4. Demo immutable SHA traceability and manual rollback path.

### Incremental Delivery

1. Foundation: Setup + Foundational.
2. Deploy automation value: US1.
3. Infrastructure resilience value: US2.
4. Security hardening value: US3.
5. Finalize cross-cutting governance and runbooks in Phase 6.

### Parallel Team Strategy

1. One stream owns Terraform foundation (T001-T010).
2. One stream owns deployment workflows (US1).
3. One stream owns network/reliability Terraform (US2).
4. One stream owns security IAM/secrets controls (US3).
