# Quickstart: Core Database and Entity Foundation

## 1. Prerequisites

- Java 17 installed.
- Docker available for local service dependencies.
- AWS credentials profile configured for Terraform planning.
- MySQL-compatible local or containerized instance for migration validation.

## 2. Scaffold Expected Structure

Create the project skeleton targeted by `plan.md` if not already present:

```text
backend/
frontend/
infra/terraform/
```

## 3. Implement Persistence Foundation

1. Add Flyway migration baseline under `backend/src/main/resources/db/migration/`:
   - `V1__init_schema.sql` for User, Deck, Card, CardLearningState with constraints.
2. Add JPA entities in `backend/src/main/java/com/khaleo/flashcard/entity/`.
3. Add DynamoDB model in `backend/src/main/java/com/khaleo/flashcard/model/dynamo/`.
4. Add optimistic-lock versioning for `CardLearningState`.
5. Add validation logic for:
   - daily limit range 1..9999 with default 9999,
   - one active learning-state row per `(userId, cardId)`,
   - card front/back content requirements.

## 4. Run Validation

Run from repository root once backend project is initialized:

```bash
cd backend
./mvnw -q -DskipTests flyway:validate
./mvnw -q test
```

## 5. Verify Critical Behaviors

- Duplicate email writes are rejected.
- Duplicate active learning-state rows for same `(userId, cardId)` are rejected.
- Concurrent state updates resolve with optimistic locking and one bounded retry.
- If DynamoDB write fails, Aurora transaction still commits and failed event is
  queued for retry/dead-letter handling.

## 6. Observability Checklist

- Structured JSON log emitted for migration start/end and failures.
- Structured JSON log emitted for async activity-log write attempts and retries.
- Structured JSON log emitted for dead-letter handoff and optimistic-lock conflict.
- New Relic and CloudWatch instrumentation points documented for persistence path.
