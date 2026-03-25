# Feature Specification: Frontend Application and Production-Gated CI/CD

**Feature Branch**: `006-frontend-and-full-cicd`  
**Created**: 2026-03-16  
**Status**: Ready for Planning  
**Input**: Project constitution and the existing five backend/infra specifications.

## Goal

Complete the product by delivering:

1. A production-ready React + Tailwind frontend for Guest, User, and Admin flows.
2. End-to-end CI/CD so operators can run quality gates and production deployments with environment-safe controls, immutable artifacts, and approval gates.

## Clarifications

### Session 2026-03-17

- Q: Which AWS authentication model should CI/CD use? -> A: Use GitHub Actions OIDC with an AWS IAM role (no long-lived access keys).
- Q: Should production deploy run automatically after push or require approval? -> A: Run CI automatically and require manual approval before production deploy.
- Q: Where should runtime secrets be stored and retrieved from? -> A: Use AWS Secrets Manager for runtime secrets.
 - Q: On deployment failure, should the pipeline auto-rollback or require manual rollback? -> A: Do not auto-rollback; mark run failed and require manual rollback.

### Session 2026-03-20

- Q: Public route policy for app entry and decks discovery? -> A: `/flashcard/decks` is public (with `/decks` alias); all other frontend routes require authenticated session.
- Q: What happens when guest clicks deck action from `/flashcard/decks`? -> A: Guest is redirected to login with return target, then returned to intended page after successful login.
- Q: What baseline test data is required? -> A: Provide seeded users/decks/cards dataset and fixed sample login accounts for manual and E2E verification.

## User Scenarios & Testing

### User Story 1 - Study Experience on Web (Priority: P1)

As a user, I can sign in, manage decks/cards, and run study sessions in a mobile-first web UI so I can learn without using API tools directly.

**Independent Test**: Open the frontend URL, complete login, create/edit deck and cards, run a study session, and verify UI and API behavior remain consistent.

**Acceptance Scenarios**:

1. **Given** a registered and verified user, **When** they log in, **Then** the app stores auth state securely and routes to the study dashboard.
2. **Given** a guest opens any route except `/decks`, **When** route guard evaluates access, **Then** the user is redirected to login and original target is preserved as return path.
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

As a release owner, I can run CI and deployment workflows with immutable artifact SHA targeting and production approval gates so releases are consistent and controlled.

**Independent Test**: Push a change to `main`, verify both workflows run, artifacts are versioned by commit SHA, deployments execute, and failures block completion.

**Acceptance Scenarios**:

1. **Given** CI workflow is triggered, **When** GitHub Actions starts, **Then** backend tests/build and frontend tests/build execute before deployment workflows.
2. **Given** successful CI and production approval, **When** deploy workflows are dispatched, **Then** backend and frontend are deployed with immutable SHA traceability.
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
- **FR-007**: CD MUST run via controlled workflow dispatch with production approval; no implicit auto-deploy is required.
- **FR-008**: Backend deployment MUST preserve immutable commit-SHA artifact semantics.
- **FR-009**: Frontend deployment MUST publish static artifacts to S3 and invalidate CloudFront cache.
- **FR-010**: CI/CD MUST support environment-specific configuration using GitHub Environments and encrypted secrets.
- **FR-011**: CI/CD MUST expose actionable failure summaries in workflow outputs.
- **FR-012**: CI/CD MUST authenticate to AWS using GitHub Actions OIDC with short-lived role assumption and no long-lived AWS access keys.
 - **FR-013**: Runtime secrets and instance-level secrets SHOULD be stored in AWS Secrets Manager and retrieved by deployed instances via IAM role.
 - **FR-014**: CI/CD MUST not perform automatic rollbacks on partial deployment failures; runs should be marked failed and require manual rollback via documented procedures.
 - **FR-015**: Deployments to production MUST require GitHub Environment approval after CI success.
- **FR-016**: Frontend route policy MUST allow unauthenticated access only to `/flashcard/decks` (and `/decks` alias); all other application routes MUST require authenticated user state.
- **FR-017**: If an unauthenticated user accesses a protected route, frontend MUST redirect to login and preserve `returnTo` so successful login navigates back to intended route.
- **FR-018**: When a guest initiates deck action from public discovery routes that requires authentication (e.g., import/copy/open private workspace), frontend MUST redirect to login with return target and resume action context after login.
- **FR-019**: Backend API policy for frontend integration MUST allow guest access only to `/api/v1/auth/**` and `GET /api/v1/public/decks/**` (plus health/docs endpoints), while all other `/api/v1/**` endpoints require JWT authentication.
- **FR-020**: Project MUST provide reusable seeded test data with fixed sample login accounts and large-scale deck coverage, including an English deck with 1500 cards for QA/E2E and performance checks.

Note: Production runtime secrets (DB credentials, JWT_SECRET, SES credentials) should be provisioned into AWS Secrets Manager and referenced by workflows and instances; deployment workflows should use short-lived role assumption via OIDC.

### Non-Functional Requirements

- **NFR-001**: Frontend initial load (P75) on standard broadband MUST be under 3 seconds for the landing page.
- **NFR-002**: Frontend MUST be responsive and usable on mobile widths >= 360px.
- **NFR-003**: Frontend Lighthouse Accessibility score MUST be >= 90 on key pages.
- **NFR-004**: CI pipeline total duration target MUST be <= 15 minutes under normal load.
- **NFR-005**: Test pyramid target remains 80% total coverage with 60/30/10 split.

## Required CI/CD Secrets and Variables

### GitHub Repository Secrets (minimum)

- `AWS_ROLE_TO_ASSUME`
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
- **SC-003**: 100% of documented CI and deployment workflows are executable with expected gating (quality checks and production approval).
- **SC-004**: At least 99% of successful runs deploy frontend and backend without manual intervention.
- **SC-005**: 100% of failed runs report enough detail to identify the failed stage and target.
- **SC-006**: 100% of guest attempts to open protected frontend routes are redirected to login with a valid return target.
- **SC-007**: 100% of documented sample accounts can successfully sign in during smoke testing, and seeded data is available for deck/card workflows without manual data entry.
