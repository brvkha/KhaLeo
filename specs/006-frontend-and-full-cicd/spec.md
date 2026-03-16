# Feature Specification: Frontend Application and Full Push-to-Deploy CI/CD

**Feature Branch**: `006-frontend-and-full-cicd`  
**Created**: 2026-03-16  
**Status**: Draft  
**Input**: Project constitution and the existing five backend/infra specifications.

## Goal

Complete the product by delivering:

1. A production-ready React + Tailwind frontend for Guest, User, and Admin flows.
2. End-to-end CI/CD so a push to `main` automatically builds, tests, and deploys both frontend and backend with environment-safe controls.

## User Scenarios & Testing

### User Story 1 - Study Experience on Web (Priority: P1)

As a user, I can sign in, manage decks/cards, and run study sessions in a mobile-first web UI so I can learn without using API tools directly.

**Independent Test**: Open the frontend URL, complete login, create/edit deck and cards, run a study session, and verify UI and API behavior remain consistent.

**Acceptance Scenarios**:

1. **Given** a registered and verified user, **When** they log in, **Then** the app stores auth state securely and routes to the study dashboard.
2. **Given** an authenticated user, **When** they CRUD decks/cards and upload media, **Then** all actions are completed via backend APIs and S3 presigned uploads.
3. **Given** a due card, **When** the user rates it (`Again`, `Hard`, `Good`, `Easy`), **Then** the UI immediately reflects next-card behavior and updated counters.

---

### User Story 2 - Admin Operations in UI (Priority: P2)

As an admin, I can use a dashboard UI to view platform stats and moderate users/decks/cards so platform operations are manageable without manual API calls.

**Independent Test**: Log in as admin, open admin dashboard, ban a user, delete a deck, edit a card, and verify authorization boundaries.

**Acceptance Scenarios**:

1. **Given** an admin user, **When** they open the admin dashboard, **Then** they see platform totals and last-24h review stats.
2. **Given** an admin, **When** they ban a user, **Then** future requests for that user fail immediately and UI shows the account as blocked.
3. **Given** a non-admin user, **When** they attempt admin routes, **Then** access is denied and redirected safely.

---

### User Story 3 - Full Automatic CI/CD on Push (Priority: P1)

As a release owner, I can push to `main` and have frontend and backend automatically built, tested, and deployed so releases are consistent and hands-off.

**Independent Test**: Push a change to `main`, verify both workflows run, artifacts are versioned by commit SHA, deployments execute, and failures block completion.

**Acceptance Scenarios**:

1. **Given** a push to `main`, **When** GitHub Actions starts, **Then** backend tests/build and frontend tests/build execute in CI before deployment steps.
2. **Given** successful CI, **When** deploy starts, **Then** backend is deployed to EC2 private instances via SSM and frontend is deployed to S3 + CloudFront invalidation.
3. **Given** one target fails, **When** workflows finish, **Then** the run is marked failed with per-target diagnostics.
4. **Given** a rollback requirement, **When** a previous commit SHA is selected, **Then** the same immutable artifact version can be redeployed.

---

## Requirements

### Functional Requirements

- **FR-001**: System MUST provide a React + Tailwind frontend application with pages for authentication, deck management, card management, study session, and profile settings.
- **FR-002**: Frontend MUST support admin routes and operations for platform stats, ban user, delete deck, and edit card.
- **FR-003**: Frontend MUST enforce route-level role guards for admin-only pages.
- **FR-004**: Frontend MUST use S3 presigned upload flow for image/audio media and enforce 5MB max size in UI validation.
- **FR-005**: Frontend MUST include simple deck search and in-deck advanced card search.
- **FR-006**: CI MUST run backend unit/integration/contract tests and frontend unit/component/E2E tests on pushes and pull requests.
- **FR-007**: CD MUST auto-deploy on push to `main` after CI passes.
- **FR-008**: Backend deployment MUST preserve immutable commit-SHA artifact semantics.
- **FR-009**: Frontend deployment MUST publish static artifacts to S3 and invalidate CloudFront cache.
- **FR-010**: CI/CD MUST support environment-specific configuration using GitHub Environments and encrypted secrets.
- **FR-011**: CI/CD MUST expose actionable failure summaries in workflow outputs.

### Non-Functional Requirements

- **NFR-001**: Frontend initial load (P75) on standard broadband MUST be under 3 seconds for the landing page.
- **NFR-002**: Frontend MUST be responsive and usable on mobile widths >= 360px.
- **NFR-003**: Frontend Lighthouse Accessibility score MUST be >= 90 on key pages.
- **NFR-004**: CI pipeline total duration target MUST be <= 15 minutes under normal load.
- **NFR-005**: Test pyramid target remains 80% total coverage with 60/30/10 split.

## Required CI/CD Secrets and Variables

### GitHub Repository Secrets (minimum)

- `AWS_ACCESS_KEY_ID`
- `AWS_SECRET_ACCESS_KEY`
- `AWS_REGION`
- `ARTIFACT_BUCKET`
- `DEPLOY_TARGET_TAG_KEY`
- `DEPLOY_TARGET_TAG_VALUE`
- `DEPLOY_SERVICE_NAME`
- `FRONTEND_S3_BUCKET`
- `CLOUDFRONT_DISTRIBUTION_ID`

### GitHub Environment Variables (example: `production`)

- `BACKEND_BASE_URL`
- `APP_ENV`
- `NODE_VERSION` (e.g. `20`)
- `JAVA_VERSION` (e.g. `17`)

## Success Criteria

- **SC-001**: 100% of P1 user flows are executable from UI without manual API tooling.
- **SC-002**: 100% of admin flows in scope are executable from UI by admin role.
- **SC-003**: 100% of pushes to `main` trigger CI/CD workflows automatically.
- **SC-004**: At least 99% of successful runs deploy frontend and backend without manual intervention.
- **SC-005**: 100% of failed runs report enough detail to identify the failed stage and target.
