# Admin/Observability/DevOps Contract: Administration, Observability, and DevOps Pipeline

## Purpose

Define external API contracts for admin operations and deployment workflow behavior required by this feature.

## General Contract Rules

- All admin endpoints are under /api/v1/admin and require authenticated admin role.
- Responses are JSON.
- Errors use the existing backend error envelope.
- This feature introduces no list-producing API; pagination clause is not applicable.

## Admin Endpoint Contracts

### 1) GET /api/v1/admin/stats

- Auth: required admin role.
- Success response:
  - 200 OK with:
    - totalUsers
    - totalDecks
    - totalCards
    - reviewsLast24Hours
    - generatedAt
- Freshness contract:
  - Response data is near-real-time with max 5 minutes lag for at least 99% of responses.
- Failure responses:
  - 401 Unauthorized for missing/invalid auth.
  - 403 Forbidden for non-admin principal.

### 2) POST /api/v1/admin/users/{userId}/ban

- Auth: required admin role.
- Success response:
  - 200 OK or 204 No Content (implementation choice, must be consistent).
- Behavior contract:
  - User is marked banned.
  - All subsequent authenticated requests from that account are denied immediately, including requests using previously issued unexpired tokens.
  - Unban API is out of scope for this feature.
- Failure responses:
  - 403 Forbidden for non-admin principal.
  - 404 Not Found for unknown user.

### 3) DELETE /api/v1/admin/decks/{deckId}

- Auth: required admin role.
- Success response:
  - 204 No Content.
- Behavior contract:
  - Hard-delete target deck regardless of visibility setting.
- Failure responses:
  - 403 Forbidden for non-admin principal.
  - 404 Not Found for unknown deck.

### 4) PUT /api/v1/admin/cards/{cardId}

- Auth: required admin role.
- Request body:
  - Mutable card content fields (same validation constraints as standard card update).
- Success response:
  - 200 OK with updated card payload.
- Failure responses:
  - 400 Bad Request for validation failure.
  - 403 Forbidden for non-admin principal.
  - 404 Not Found for unknown card.

## Admin Auditability Contract

- Each moderation action must emit an auditable event with:
  - actor admin identity
  - action type
  - target type and identifier
  - timestamp
  - outcome
- Audit failures must be observable to operators.

## Observability Contract

- Runtime logs must remain structured JSON.
- Splunk integration must use asynchronous HEC delivery to avoid request-thread blocking.
- New Relic APM must be enabled through javaagent runtime configuration.
- Required observable outcomes:
  - admin authorization denials
  - admin moderation successes/failures
  - deployment command dispatch and per-target results
  - log-shipping failure conditions

## Deployment Workflow Contract (.github/workflows/deploy-backend.yml)

- Trigger:
  - push to main.
- Build:
  - Java 17 + Maven build produces deployable backend artifact.
- Artifact:
  - Publish immutable commit-SHA keyed artifact to S3.
- Deploy:
  - Use aws ssm send-command targeting EC2 instances by tags.
  - Dispatch deployment to all intended targets in the run.
- Completion semantics:
  - Workflow is marked failed if any target fails, even when others succeed.
  - Per-target outcome reporting is required in workflow summary/logs.
- Rollback:
  - Previously published commit-SHA artifact can be redeployed without rebuild.

## Error Envelope Contract

Error responses follow existing JSON envelope:

- timestamp
- status
- error
- message
- path

### Feature-specific error codes

- AUTHORIZATION_DENIED
- BANNED_USER_REQUEST_DENIED
- USER_NOT_FOUND
- DECK_NOT_FOUND
- CARD_NOT_FOUND
- ADMIN_ACTION_FAILED
- DEPLOY_TARGET_FAILURE
- DEPLOY_ARTIFACT_NOT_FOUND
