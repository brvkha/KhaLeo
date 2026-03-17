# Tasks: Frontend Application and Full Push-to-Deploy CI/CD

**Input**: Design docs from `/specs/006-frontend-and-full-cicd/`  
**Prerequisites**: `plan.md` (required), `spec.md` (required), `quickstart.md`

## Phase 1: Setup

**Purpose**: Align repository configuration to the approved deployment/security model before implementation.

**Acceptance Checks**:
- Terraform outputs needed by CI/CD are documented and mapped to GitHub secrets/variables.
- OIDC-only authentication model is reflected in deployment docs (no static AWS access keys).
- Frontend workspace has baseline tooling and environment contracts.

- [X] T001 [P] Create CI/CD variable mapping table from Terraform outputs in specs/006-frontend-and-full-cicd/quickstart.md (Owner: DevOps)
- [X] T002 Replace access-key-based auth guidance with GitHub OIDC role assumption in specs/006-frontend-and-full-cicd/quickstart.md (Owner: DevOps)
- [X] T003 Add/update production environment variable contract for backend/frontend in .github/workflows/ci.yml (Owner: DevOps)
- [ ] T004 [P] Confirm frontend environment sample and runtime contract in frontend/.env.example (Owner: Frontend)

---

## Phase 2: CI/CD Scope (User Story 3 - Priority: P1)

**Goal**: Push to `main` executes CI automatically and allows production deployment only after environment approval.

**Independent Test**: Push to `main` runs CI, waits on `production` approval gate, and deploys immutable SHA artifacts for backend/frontend after approval.

**Acceptance Checks**:
- CI runs backend + frontend quality gates on PR and push.
- Deploy workflows use OIDC role assumption and do not require AWS long-lived keys.
- Partial deployment failures mark workflow failed without auto-rollback.

- [X] T005 [US3] Implement backend+frontend CI gate workflow with explicit quality stages in .github/workflows/ci.yml (depends on T003, Owner: DevOps)
- [X] T006 [US3] Enforce OIDC credential step and production environment approval in .github/workflows/deploy-backend.yml (depends on T002, T005, Owner: DevOps)
- [ ] T007 [US3] Create frontend deploy workflow (build, S3 sync, CloudFront invalidation) in .github/workflows/deploy-frontend.yml (depends on T001, T002, T005, Owner: DevOps)
- [ ] T008 [US3] Preserve immutable commit-SHA artifact handling and manual rollback dispatch inputs in .github/workflows/deploy-backend.yml (depends on T006, Owner: DevOps)
- [ ] T009 [P] [US3] Add workflow failure summaries and per-target diagnostics in .github/workflows/deploy-backend.yml (depends on T006, Owner: DevOps)
- [ ] T010 [P] [US3] Add workflow failure summaries and per-target diagnostics in .github/workflows/deploy-frontend.yml (depends on T007, Owner: DevOps)
- [ ] T011 [US3] Update deploy scripts to consume Secrets Manager-backed runtime configuration in backend/scripts/deploy-via-ssm.sh (depends on T006, Owner: Backend)

---

## Phase 3: Frontend Scope (User Story 1 - Priority: P1)

**Goal**: Deliver user-facing web study experience with secure auth, deck/card management, media upload, and study flow.

**Independent Test**: User signs in, performs deck/card CRUD including media upload, completes a study round with rating actions, and sees consistent state updates.

**Acceptance Checks**:
- Auth state and protected routing are enforced.
- Deck/card CRUD and search are usable on mobile width >= 360px.
- Presigned upload validates 5MB max before backend calls.

- [ ] T012 [P] [US1] Implement auth pages and session bootstrap in frontend/src/features/auth/ (depends on T004, Owner: Frontend)
- [ ] T013 [US1] Implement route guards and role-aware redirects in frontend/src/router/guards.tsx (depends on T012, Owner: Frontend)
- [ ] T014 [P] [US1] Implement deck CRUD pages and query hooks in frontend/src/features/decks/ (depends on T012, Owner: Frontend)
- [ ] T015 [P] [US1] Implement card CRUD pages and query hooks in frontend/src/features/cards/ (depends on T012, Owner: Frontend)
- [ ] T016 [US1] Implement deck search and in-deck advanced card search in frontend/src/features/search/ (depends on T014, T015, Owner: Frontend)
- [ ] T017 [US1] Implement S3 presigned media upload flow with 5MB validation in frontend/src/features/media/ (depends on T015, Owner: Frontend)
- [ ] T018 [US1] Implement study session interaction flow (reveal/rate/progress) in frontend/src/features/study/ (depends on T014, T015, Owner: Frontend)
- [ ] T019 [US1] Implement account profile and daily-learning-limit settings UI in frontend/src/features/profile/ (depends on T012, Owner: Frontend)

---

## Phase 4: Frontend Scope (User Story 2 - Priority: P2)

**Goal**: Deliver admin dashboard and moderation operations with strict role boundaries.

**Independent Test**: Admin can view platform stats and moderate users/decks/cards; non-admin users are denied and redirected.

**Acceptance Checks**:
- Admin routes require admin role.
- Ban and moderation actions surface clear confirmation and failure states.
- Non-admin access attempts are safely rejected.

- [ ] T020 [P] [US2] Implement admin dashboard stats screen in frontend/src/features/admin/dashboard/ (depends on T013, Owner: Frontend)
- [ ] T021 [US2] Implement admin user moderation (ban/unban) in frontend/src/features/admin/users/ (depends on T020, Owner: Frontend)
- [ ] T022 [US2] Implement admin deck moderation (delete any deck) in frontend/src/features/admin/decks/ (depends on T020, Owner: Frontend)
- [ ] T023 [US2] Implement admin card moderation (edit any card) in frontend/src/features/admin/cards/ (depends on T020, Owner: Frontend)
- [ ] T024 [US2] Add audit-friendly confirmation dialogs and error states for admin actions in frontend/src/features/admin/components/ (depends on T021, T022, T023, Owner: Frontend)

---

## Phase 5: Validation

**Purpose**: Enforce test and quality gates for frontend and deployment pipeline behavior.

**Acceptance Checks**:
- Frontend unit/component/E2E suites run in CI.
- CI output includes coverage and stage-level diagnostics.
- End-to-end push-to-deploy dry run is repeatable.

- [ ] T025 [P] Add frontend unit tests for auth hooks, guard logic, and state transitions in frontend/src/**/*.test.ts (depends on T013, Owner: Frontend)
- [ ] T026 [P] Add frontend component/integration tests for deck/card/study journeys in frontend/src/**/*.test.tsx (depends on T018, Owner: Frontend)
- [ ] T027 Add frontend E2E smoke tests for P1 flows in frontend/tests/e2e/ (depends on T019, Owner: Frontend)
- [ ] T028 Add CI coverage publication and threshold enforcement in .github/workflows/ci.yml (depends on T005, T025, T026, Owner: DevOps)
- [ ] T029 Add frontend-backend DTO contract drift checks in frontend/src/services/contracts/ (depends on T014, T015, T018, Owner: Frontend)
- [ ] T030 Execute and record push-to-main deployment validation checklist in specs/006-frontend-and-full-cicd/quickstart.md (depends on T008, T010, T011, T027, Owner: DevOps)

---

## Phase 6: Docs

**Purpose**: Finalize operational runbooks and handoff material.

**Acceptance Checks**:
- On-call can execute deploy and rollback by SHA using documentation only.
- Secret ownership and rotation responsibilities are explicit.
- Feature docs reflect final architecture decisions and constraints.

- [ ] T031 Update CI/CD runbook with OIDC role ARN, required env vars, and approval flow in KhaLeoDocs/cicd_runbook_and_iam_policies.md (depends on T006, T007, Owner: DevOps)
- [ ] T032 Update frontend delivery and testing runbook in frontend/README.md (depends on T019, T027, Owner: Frontend)
- [ ] T033 Update production rollback-by-SHA procedure in specs/006-frontend-and-full-cicd/quickstart.md (depends on T008, T030, Owner: DevOps)
- [ ] T034 Add release readiness checklist for frontend + backend deployment in README.md (depends on T031, T032, T033, Owner: DevOps)

---

## Dependencies & Execution Order

### Phase Dependencies

- Phase 1 (Setup): No dependencies.
- Phase 2 (CI/CD scope): Depends on Phase 1 and unblocks production deployment path.
- Phase 3 (Frontend US1): Depends on Phase 1; can progress in parallel with late CI/CD hardening after T005.
- Phase 4 (Frontend US2): Depends on Phase 3 guard/auth foundations.
- Phase 5 (Validation): Depends on completion of relevant CI/CD + frontend implementation tasks.
- Phase 6 (Docs): Depends on validated behavior from Phases 2-5.

### Task-Level Critical Path

- T001 -> T007 -> T010 -> T030 -> T033 -> T034
- T003 -> T005 -> T006 -> T008 -> T030
- T004 -> T012 -> T013 -> T020 -> T021/T022/T023 -> T024
- T012 -> T014/T015 -> T016/T017/T018 -> T026 -> T027

### Parallel Opportunities

- T001 and T004 can run in parallel during Setup.
- T009 and T010 can run in parallel after deploy workflows are in place.
- T014 and T015 can run in parallel after auth bootstrap.
- T025 and T026 can run in parallel once frontend flows stabilize.

## Suggested MVP Scope

- MVP target: Complete Phase 1 + Phase 2 + Phase 3 + T025/T026/T028 + T031/T033.
- This ships P1 value: user web study experience and governed push-to-deploy path with manual production approval.

## Format Validation

- All tasks follow checklist format: `- [ ] T### [P?] [US?] Description with file path`.
- User story labels are applied to story-specific phases (US1, US2, US3).
- Every task includes ownership hint and dependency context where required.
