# Quickstart: SM-2 Spaced Repetition and Study Activity Logging

## 1. Prerequisites

- Java 17 installed.
- Maven available.
- Docker running for integration-test dependencies.
- AWS credentials configured for DynamoDB access in target environment.

## 2. Implement Scheduling Domain Logic

1. Add/extend domain service under `backend/src/main/java/com/khaleo/flashcard/service/study/`:
   - Apply SM-2 formulas for `AGAIN`, `HARD`, `GOOD`, `EASY`.
   - Enforce `easeFactor >= 1.3`.
   - Implement state transitions for `NEW`, `LEARNING`, `MASTERED`, `REVIEW`.
2. Ensure first and second `GOOD` learning-step behavior is explicit:
   - first `GOOD` on `NEW` -> 10-minute relearning timer.
   - second qualifying `GOOD` on `LEARNING` -> 1-day mastered waiting interval.

## 3. Implement Study APIs

1. Add next-cards endpoint under `backend/src/main/java/com/khaleo/flashcard/controller/study/`:
   - `GET /api/v1/study/decks/{deckId}/next-cards`
   - Enforce ordering tiers and daily new-card limit.
   - Support `size` and `continuationToken` pagination.
2. Add card-rating endpoint:
   - `POST /api/v1/study/cards/{cardId}/rate`
   - Validate rating/time payload.
   - Persist updated scheduling state.

## 4. Implement Async DynamoDB Activity Logging

1. Add `ActivityLogService` under `backend/src/main/java/com/khaleo/flashcard/service/activitylog/`:
   - Use AWS SDK v2 DynamoDB Enhanced Client.
   - Asynchronously persist `StudyActivityLog` entries after successful rating updates.
2. Ensure logging failures are observable and non-fatal to rating success path.

## 5. Observability and Reliability

1. Add structured logs and metrics for:
   - next-cards success/failure and authorization denials.
   - rating success/failure outcomes.
   - async DynamoDB write success/failure outcomes.
2. Add or update CloudWatch/New Relic alarm mappings in Terraform if new metrics are introduced.

## 6. Run Verification

From repository root:

```bash
cd backend
mvn -q -DskipTests flyway:validate
mvn test
```

Targeted test examples:

```bash
mvn -Dtest="*Study*" test
mvn -Dtest="*SpacedRepetition*" test
```

## 7. Validate Critical Behaviors

- Next-cards ordering always prioritizes due learning, then due review, then new eligible cards.
- Daily unique new-card limit blocks additional new cards once quota is reached.
- SM-2 calculations and minimum ease-factor clamp are deterministic.
- Rating API remains responsive even when activity-log write path is slow/failing.
- Async logging failures are visible in telemetry.

## 8. Evidence To Capture

- Contract, integration, and unit test reports for study endpoints and scheduling service.
- Flyway validation output from build logs.
- Sample structured logs/metrics for async logging success/failure.
- Terraform diff for any DynamoDB/alarm updates.

## 9. Verification Evidence (2026-03-16)

- Targeted regression for US2/US3 fixes:
   - Command: `mvn -Dtest="com.khaleo.flashcard.integration.study.StudyRateCardTransitionIT,com.khaleo.flashcard.unit.study.StudyActivityLogPublisherTest,com.khaleo.flashcard.integration.ActivityLogPublishIT,com.khaleo.flashcard.integration.ActivityLogRetryDeadLetterIT" test`
   - Result: `Tests run: 6, Failures: 0, Errors: 0, Skipped: 0`.
- Full backend validation:
   - Command: `mvn test`
   - Surefire aggregate from XML reports: `Tests=56 Failures=0 Errors=0 Skipped=0 TimeSeconds=73.212`.
- Runtime behavior observed in logs during test execution:
   - Async activity log publish success events emitted.
   - Retry and dead-letter events emitted when DynamoDB was intentionally unavailable in resilience tests.
