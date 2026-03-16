# Data Model: Administration, Observability, and DevOps Pipeline

## Relational Entities (Aurora MySQL)

### 1. User (existing entity, feature extension)
- Purpose: Account identity and authorization state.
- Key fields:
  - id (UUID, PK)
  - email (string, unique)
  - role (enum, includes ROLE_ADMIN)
  - accountLockedUntil (timestamp, existing lockout field)
  - isEmailVerified (boolean)
  - failedLoginAttempts (int)
  - bannedAt (timestamp, nullable, optional feature extension)
  - bannedBy (UUID, nullable, optional feature extension)
- Validation rules:
  - role is required.
  - banned state must be enforceable on every authenticated request.
- Relationships:
  - One-to-many to Deck (author).
  - One-to-many to refresh/verification/reset token entities.

### 2. AdminModerationAction (new relational audit entity or auditable log projection)
- Purpose: Immutable audit trail for admin actions.
- Key fields:
  - actionId (UUID, PK)
  - adminUserId (UUID, required)
  - actionType (enum: USER_BAN, DECK_DELETE, CARD_EDIT)
  - targetType (enum: USER, DECK, CARD)
  - targetId (UUID, required)
  - status (enum: SUCCESS, FAILURE)
  - reasonCode (string, optional)
  - createdAt (timestamp)
- Validation rules:
  - actionType and targetType are required.
  - targetId must match targetType resource identity.
  - Record is immutable after creation.

### 3. PlatformStatsSnapshot (derived/projection model)
- Purpose: Return admin stats payload with bounded freshness.
- Attributes:
  - totalUsers
  - totalDecks
  - totalCards
  - reviewsLast24Hours
  - generatedAt
- Validation rules:
  - generatedAt must be within 5 minutes of response time for 99% of requests.

## Artifact and Deployment Entities

### 4. DeploymentArtifact (S3 object contract)
- Purpose: Immutable backend artifact reference for deployment.
- Attributes:
  - artifactKey (string, includes commit SHA)
  - commitSha (string)
  - createdAt (timestamp)
  - checksum (string, optional)
- Validation rules:
  - artifactKey is immutable once published.
  - commitSha in key must match workflow commit.

### 5. DeploymentCommandExecution (SSM command result model)
- Purpose: Per-target deploy outcome tracking.
- Attributes:
  - commandId
  - targetInstanceId
  - status (SUCCESS, FAILED, TIMED_OUT, CANCELLED)
  - startedAt
  - finishedAt
  - errorSummary (optional)
- Validation rules:
  - Workflow succeeds only when all intended targets report SUCCESS.
  - Any failed target marks overall run failed after full-target dispatch completes.

## Observability Data Contracts

### 6. OperationalLogEvent (JSON log event)
- Purpose: Structured logs for Splunk and operational analysis.
- Required fields:
  - timestamp
  - level
  - app
  - thread
  - logger
  - message
  - exception
- Validation rules:
  - Must remain valid JSON per line.
  - Must not expose secrets (tokens, keys, passwords).

## State Transitions

### User moderation state
- ACTIVE -> BANNED when admin ban action succeeds.
- BANNED -> ACTIVE is out of scope for this feature (handled externally/future).
- Request-time authorization checks must deny all authenticated requests for BANNED users.

### Deployment execution state
- CREATED -> DISPATCHING -> TARGET_COMPLETED (per target) -> RUN_FAILED or RUN_SUCCEEDED.
- Rule: RUN_FAILED if at least one target is FAILED/TIMED_OUT/CANCELLED.

## Deterministic Validation Outcomes

- AUTHORIZATION_DENIED
- USER_NOT_FOUND
- DECK_NOT_FOUND
- CARD_NOT_FOUND
- BANNED_USER_REQUEST_DENIED
- ADMIN_ACTION_AUDIT_WRITE_FAILED (non-blocking if business action already committed, but must be observable)
- DEPLOY_TARGET_FAILURE
- DEPLOY_ARTIFACT_NOT_FOUND

## Consistency and Failure Handling

- Admin moderation writes in Aurora are primary transactional outcomes.
- Audit/event observability writes may be asynchronous but must be detectable.
- Logging transport failure to Splunk must not block API request completion.
- Deployment must attempt all targets and then compute final run status from per-target results.
