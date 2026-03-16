# Tasks: Frontend Application and Full Push-to-Deploy CI/CD

**Input**: Design docs from `/specs/006-frontend-and-full-cicd/`  
**Prerequisites**: `spec.md`, `plan.md`

## Phase 1: Frontend Foundation

- [ ] T001 Create frontend scaffold (`frontend/`) with Vite + React + TypeScript + Tailwind.
- [ ] T002 Add routing shell and layout primitives for public/auth/app/admin areas.
- [ ] T003 Add API client module with auth interceptors and typed error mapping.
- [ ] T004 Add state/query setup (TanStack Query + global auth store).
- [ ] T005 Add frontend env config contract (`VITE_API_BASE_URL`, `VITE_APP_ENV`).

## Phase 2: Authentication and Shared UX

- [ ] T006 Implement login/register/verify/reset pages and forms.
- [ ] T007 Implement protected-route guards for authenticated users.
- [ ] T008 Implement role guard for admin routes.
- [ ] T009 Add global notification and error boundary behavior.
- [ ] T010 Add responsive navigation and mobile-first shell.

## Phase 3: Core User Domain UI

- [ ] T011 Implement deck list/create/edit/delete pages with pagination.
- [ ] T012 Implement card list/create/edit/delete pages per deck with pagination.
- [ ] T013 Implement simple deck search and in-deck advanced card search.
- [ ] T014 Implement S3 presigned upload flow UI (<= 5MB validation).
- [ ] T015 Implement study session UI (front -> reveal -> rating controls).
- [ ] T016 Implement account daily-limit settings page.

## Phase 4: Admin UI

- [ ] T017 Implement admin dashboard stats page.
- [ ] T018 Implement user moderation panel (ban action).
- [ ] T019 Implement admin deck moderation actions (delete any deck).
- [ ] T020 Implement admin card moderation actions (edit any card).
- [ ] T021 Add audit-friendly admin action confirmation dialogs.

## Phase 5: Test & Quality Gates

- [ ] T022 Add frontend unit tests for key hooks/components.
- [ ] T023 Add frontend integration/component tests for auth + deck + study flows.
- [ ] T024 Add frontend E2E tests for P1 user journeys.
- [ ] T025 Add coverage reporting and CI upload.
- [ ] T026 Add API contract drift checks between frontend DTOs and backend responses.

## Phase 6: Full CI/CD Automation

- [ ] T027 Create `.github/workflows/ci.yml` for backend + frontend test/build gates.
- [ ] T028 Update `.github/workflows/deploy-backend.yml` to depend on CI success and keep SHA-based deployment.
- [ ] T029 Create `.github/workflows/deploy-frontend.yml` to build frontend and deploy to S3.
- [ ] T030 Add CloudFront invalidation step in frontend deploy workflow.
- [ ] T031 Add workflow summaries and artifact retention for troubleshooting.
- [ ] T032 Add rollback workflow dispatch by commit SHA for frontend and backend.

## Phase 7: Ops Documentation

- [ ] T033 Add final runbook for GitHub secrets, environment variables, and IAM permissions.
- [ ] T034 Document one-command local dev bootstrap for backend + frontend.
- [ ] T035 Add release checklist (pre-merge checks, post-deploy verification, rollback).

## Execution Order

1. Finish Phase 1 and Phase 2.
2. Deliver Phase 3 (user value) and validate.
3. Deliver Phase 4 (admin value) and validate.
4. Complete Phase 5 quality gates.
5. Implement Phase 6 CI/CD.
6. Finalize Phase 7 documentation and handoff.
