# Data Model: Authentication, Security, and Identity Verification

## Relational Entities (Aurora MySQL)

### 1. User (extended)
- Purpose: Identity root with verification, login-failure, and lockout state.
- Existing fields retained from feature 001 plus auth extensions.
- Auth-related fields:
  - `email` (VARCHAR, unique, not null)
  - `passwordHash` (VARCHAR, not null)
  - `isEmailVerified` (BOOLEAN, not null, default `false`)
  - `failedLoginAttempts` (INT, not null, default `0`)
  - `accountLockedUntil` (TIMESTAMP, nullable)
  - `createdAt` (TIMESTAMP, not null)
  - `updatedAt` (TIMESTAMP, not null)
- Validation rules:
  - `failedLoginAttempts` must be >= 0.
  - Account is considered locked when `accountLockedUntil` is in the future.
  - Successful authentication resets `failedLoginAttempts` to `0`.
- Relationships:
  - One-to-many with `RefreshToken`.
  - One-to-many with `EmailVerificationToken`.
  - One-to-many with `PasswordResetToken`.

### 2. RefreshToken
- Purpose: Persist long-lived refresh credential for rotation/revocation checks.
- Fields:
  - `id` (UUID, PK)
  - `token` (VARCHAR, unique, not null)
  - `userId` (UUID, FK -> User.id, not null)
  - `expiresAt` (TIMESTAMP, not null)
  - `revokedAt` (TIMESTAMP, nullable)
  - `createdAt` (TIMESTAMP, not null)
  - `updatedAt` (TIMESTAMP, not null)
- Validation rules:
  - `expiresAt` must be later than creation time.
  - Token can be used only when not expired and `revokedAt` is null.
- Relationships:
  - Many-to-one with `User`.

### 3. EmailVerificationToken
- Purpose: One-time proof token for email ownership confirmation.
- Fields:
  - `id` (UUID, PK)
  - `token` (VARCHAR, unique, not null)
  - `userId` (UUID, FK -> User.id, not null)
  - `expiresAt` (TIMESTAMP, not null, max 24h from issue)
  - `consumedAt` (TIMESTAMP, nullable)
  - `createdAt` (TIMESTAMP, not null)
  - `updatedAt` (TIMESTAMP, not null)
- Validation rules:
  - Token is valid only when current time < `expiresAt` and `consumedAt` is null.
  - Successful verification sets `consumedAt` and flips `User.isEmailVerified` to true.
- Relationships:
  - Many-to-one with `User`.

### 4. PasswordResetToken
- Purpose: One-time credential for password reset completion.
- Fields:
  - `id` (UUID, PK)
  - `token` (VARCHAR, unique, not null)
  - `userId` (UUID, FK -> User.id, not null)
  - `expiresAt` (TIMESTAMP, not null, max 1h from issue)
  - `consumedAt` (TIMESTAMP, nullable)
  - `createdAt` (TIMESTAMP, not null)
  - `updatedAt` (TIMESTAMP, not null)
- Validation rules:
  - Token is valid only when current time < `expiresAt` and `consumedAt` is null.
  - Successful reset consumes token and revokes all active user refresh tokens.
- Relationships:
  - Many-to-one with `User`.

## Derived State Rules

- Login lockout:
  - Failure increments `failedLoginAttempts`.
  - On transition to count `5`, set `accountLockedUntil = now + 24h`.
  - Sign-in attempts while `accountLockedUntil > now` are rejected.
- Verification prerequisite:
  - Access/refresh tokens are never issued for `isEmailVerified = false` accounts.
- Session invalidation:
  - Password reset completion revokes every unexpired refresh token for the user.

## State Transitions

### Account Verification State
- `UNVERIFIED -> VERIFIED` on successful email-verification token consumption.

### Account Access State
- `ACTIVE -> LOCKED` on fifth consecutive failed login.
- `LOCKED -> ACTIVE` after lockout expiration and next valid login.

### Token Lifecycle State
- Refresh token: `ACTIVE -> REVOKED` on logout/reset; `ACTIVE -> EXPIRED` on TTL end.
- Verification/reset token: `ISSUED -> CONSUMED` on valid use; `ISSUED -> EXPIRED` on TTL end.

## Deterministic Validation Outcomes

- `DUPLICATE_EMAIL`
- `UNVERIFIED_EMAIL`
- `ACCOUNT_LOCKED`
- `INVALID_CREDENTIALS`
- `INVALID_REFRESH_TOKEN`
- `EXPIRED_REFRESH_TOKEN`
- `INVALID_VERIFICATION_TOKEN`
- `EXPIRED_VERIFICATION_TOKEN`
- `INVALID_RESET_TOKEN`
- `EXPIRED_RESET_TOKEN`

## Non-Relational Data Impact

- No new DynamoDB entities are introduced for this feature.
- Existing `StudyActivityLog` data model and contracts remain unchanged.

## Implementation Status

- User lockout and verification fields are implemented in relational schema and entity model.
- Refresh, verification, and password reset token entities are implemented with one-time and expiry behavior.
- Password reset flow revokes all active refresh tokens for the affected user.
