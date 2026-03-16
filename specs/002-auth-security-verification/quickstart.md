# Quickstart: Authentication, Security, and Identity Verification

## 1. Prerequisites

- Java 17 installed.
- Maven available.
- Docker running for integration-test dependencies.
- AWS credentials/environment configured for SES integration and Terraform planning.

## 2. Implement Data Layer Extensions

1. Add Flyway migration(s) under `backend/src/main/resources/db/migration/` for:
   - User auth extensions (`failedLoginAttempts`, `accountLockedUntil`).
   - `refresh_token` table.
   - `email_verification_token` table.
   - `password_reset_token` table.
2. Add/update JPA entities and repositories under:
   - `backend/src/main/java/com/khaleo/flashcard/entity/`
   - `backend/src/main/java/com/khaleo/flashcard/repository/`

## 3. Implement Security and Auth Flows

1. Configure Spring Security for stateless JWT auth:
   - Permit `/api/v1/auth/**`.
   - Require authentication for other `/api/v1/**` routes.
2. Add JWT generation/validation utilities with:
   - Access token TTL 15 minutes.
   - Refresh token TTL 7 days.
3. Implement auth service/controller endpoints for:
   - register, verify, login, refresh, forgot-password, reset-password, logout.
4. Enforce login lockout and unverified-account restrictions.
5. Integrate SES email dispatch for verification/reset messages.

## 4. Observability and Error Handling

1. Add standardized JSON error responses for auth exceptions.
2. Emit structured security event logs for all auth outcomes.
3. Ensure no secrets or token raw values are logged.
4. Add/adjust New Relic and CloudWatch instrumentation/alarm mappings if auth-path telemetry is expanded.

## 5. Run Verification

From repository root:

```bash
cd backend
mvn -q -DskipTests flyway:validate
mvn test
```

Validation evidence command used in Phase 6:

```bash
# Workspace test runner equivalent summary
# passed=61 failed=0
```

## 6. Validate Critical Behaviors

- Unverified users cannot authenticate.
- Verification tokens work once and expire at 24h.
- Login locks account exactly after 5th consecutive failure for 24h.
- Refresh tokens are rejected when expired/revoked.
- Password reset invalidates active refresh tokens for that user.
- Logout revokes provided refresh token idempotently.

## 7. Evidence To Capture

- Contract and integration test reports for auth endpoints.
- Migration validation output.
- Security-event log samples (sanitized).
- Any CloudWatch/New Relic/Splunk config changes linked in PR notes.
- Phase 6 summary report at `backend/build/reports/tests/phase6-test-and-coverage-summary.md`.
