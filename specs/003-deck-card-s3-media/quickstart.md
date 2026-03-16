# Quickstart: Deck/Card Management and Media Uploads

## 1. Prerequisites

- Java 17 installed.
- Maven available.
- Docker running for MySQL Testcontainers-based integration tests.
- AWS credentials/profile configured for S3 presigner validation in non-local environments.

## 2. Implement Persistence and Query Layer

1. Extend repositories in `backend/src/main/java/com/khaleo/flashcard/repository/`:
   - paginated deck listing and filtering (`isPublic`, `authorId`).
   - deck-scoped card search with case-insensitive contains/exact semantics.
2. Add/update service methods in `RelationalPersistenceService` for:
   - deck read/list/update/delete.
   - card read/search/update/delete.
3. Add ownership/admin authorization helper used by all mutation methods.
4. Ensure deck/card deletion path handles dependent learning states and media reference updates.

## 3. Implement API Layer Contracts

1. Add controllers and DTOs under `backend/src/main/java/com/khaleo/flashcard/controller/` for:
   - `/api/v1/decks` CRUD + list.
   - `/api/v1/decks/{deckId}/cards/search`.
   - `/api/v1/cards/{id}` update/delete.
   - `/api/v1/media/presigned-url`.
2. Enforce pagination defaults and max page size.
3. Return deterministic error envelope codes for authorization, validation, and rate limits.

## 4. Implement S3 Presigned Media Authorization

1. Add AWS SDK v2 dependencies for S3 and S3 presigner in `backend/pom.xml`.
2. Add S3 config/service classes under `backend/src/main/java/com/khaleo/flashcard/config/` and `.../service/`:
   - validate file metadata (`contentType`, extension, `sizeBytes <= 5MB`).
   - issue 5-minute presigned PUT URL and stable object key.
3. Implement per-user rate limiting at 30 requests/minute for media authorization.
4. Persist or emit sufficient data to support reference-aware media deletion behavior.

## 5. Observability and Security Hardening

1. Emit structured events for deck/card and media-authorization outcomes.
2. Extend New Relic instrumentation for:
   - authorization denials
   - validation failures
   - rate-limit rejections
   - mutation success/failure
3. If needed, add CloudWatch alarms through `infra/terraform/*.tf` only.
4. Verify no secrets, JWTs, or signed URL secrets are logged.

## 6. Run Verification

From repository root:

```bash
cd backend
mvn -q -DskipTests flyway:validate
mvn test
```

## 7. Validate Critical Behaviors

- Public deck/card reads succeed without authentication.
- Private deck/card reads are denied for non-owner/non-admin users.
- Deck/card mutation operations are denied for non-owner/non-admin users.
- Deck and card list/search endpoints return paginated results with deterministic defaults.
- Card search semantics:
  - front/back: case-insensitive contains.
  - vocabulary: case-insensitive exact.
- Media authorization:
  - allows only supported types and <=5MB size.
  - returns presigned URL valid for 5 minutes.
  - rejects >30 requests/user/minute with rate-limit error.
- Media object deletion occurs only when no remaining references exist.

## 8. Evidence To Capture

- Contract tests for deck/card/media endpoints and error envelopes.
- Integration tests for ownership checks, visibility rules, pagination, and search behavior.
- Unit tests for media validation, URL expiry generation, and rate limiter.
- Migration validation output and (if applicable) Flyway migration tests.
- Terraform plan output for any new alarm metrics.

## 9. Verification Log (2026-03-16)

- `mvn -q -DskipTests flyway:validate` executed successfully in `backend/`.
- Targeted US1 suites passed: deck contract/integration/unit tests.
- Targeted US2 suites passed: card contract/integration/search/unit tests.
- Targeted US3 suites passed: media authorization contract/integration/rate-limit/lifecycle/unit tests.
- Full backend test run completed successfully and summarized in `backend/build/reports/tests/phase-deck-card-media-summary.md`.

### Revalidation Update (2026-03-16, current environment)

- `mvn -q -DskipTests flyway:validate` failed in local environment because Flyway Maven plugin DB URL/user/password were not configured.
- Full backend regression rerun passed after integration test alignment: `108 passed, 0 failed`.
- Terraform checks: `terraform fmt -check -recursive` passed; `terraform validate` could not complete because provider download from `registry.terraform.io` timed out in the current network environment.
