# Data Model: FSRS v4 Spaced Repetition and Study Activity Logging

## Relational Entities (Aurora MySQL)

### 1. CardLearningState (existing entity, feature extension)
- Purpose: Authoritative per-user scheduling state for each card.
- Key fields:
  - `id` (UUID, PK)
  - `userId` (UUID, not null)
  - `cardId` (UUID, not null)
  - `state` (ENUM: `NEW`, `LEARNING`, `REVIEW`, `RELEARNING`; historical `MASTERED` supported for backward compatibility)
  - `fsrsStability` (DOUBLE, not null, default 0)
  - `fsrsDifficulty` (DOUBLE, not null, default 0)
  - `fsrsElapsedDays` (INT, not null, default 0)
  - `fsrsScheduledDays` (INT, not null, default 0)
  - `fsrsReps` (INT, not null, default 0)
  - `fsrsLapses` (INT, not null, default 0)
  - `lastReviewedAt` (TIMESTAMP, nullable)
  - `nextReviewAt` (TIMESTAMP, nullable)
  - `learningStepGoodCount` (INT, not null, default 0, retained for compatibility)
  - `createdAt` (TIMESTAMP, not null)
  - `updatedAt` (TIMESTAMP, not null)
- Validation rules:
  - `fsrsDifficulty` is clamped to [1, 10] once initialized.
  - `fsrsStability >= 0.1` for non-new cards once initialized.
  - `fsrsElapsedDays >= 0`, `fsrsScheduledDays >= 0`, `fsrsReps >= 0`, `fsrsLapses >= 0`.
  - `state = NEW` implies no completed study action yet for that user/card pair.
  - `learningStepGoodCount` is no longer authoritative for scheduling decisions under FSRS.
- Relationships:
  - Many-to-one with `User`.
  - Many-to-one with `Card`.
  - Unique constraint on (`userId`, `cardId`).

### 2. StudyDailyQuota (derived relational projection)
- Purpose: Efficiently determine how many unique cards a user first studied today.
- Representation:
  - Derived from first-study timestamps in `CardLearningState` or equivalent study progress table.
- Derived attributes:
  - `userId`
  - `studyDate` (server timezone date)
  - `uniqueCardsFirstStudiedCount`
- Validation rules:
  - Count must include each card at most once per user per day.
  - Repeated reviews of already-counted cards do not increase daily unique count.

## Non-Relational Entity (DynamoDB)

### 3. StudyActivityLogItem
- Purpose: Immutable analytics/audit event for each accepted rating action.
- Table: `StudyActivityLog`
- Keys:
  - `logId` (UUID, partition key)
  - `timestamp` (ISO-8601 instant, sort key)
- Attributes:
  - `userId` (string, required, GSI partition key)
  - `cardId` (string, required)
  - `deckId` (string, required)
  - `ratingGiven` (string enum: `AGAIN`, `HARD`, `GOOD`, `EASY`)
  - `timeSpentMs` (number, required)
  - `scheduledDays` (number, required)
  - `newStability` (number, required)
  - `newDifficulty` (number, required)
- Validation rules:
  - `timeSpentMs >= 0`.
  - `newStability >= 0.1`.
  - `newDifficulty` is in [1, 10].
  - Immutable after write.

## State Transition Rules

### Card state transitions on rating
- `NEW` + `AGAIN|HARD|GOOD` -> `LEARNING`; initialize `D0` and `S0` from FSRS parameter vector.
- `NEW` + `EASY` -> `REVIEW`; initialize `D0`/`S0` and schedule by calculated days.
- `REVIEW` + `AGAIN` -> `RELEARNING`; increment `fsrsLapses` and compute stability by FSRS forget formula.
- `REVIEW` + `HARD|GOOD|EASY` -> `REVIEW`; compute retrievability and update stability by FSRS recall formula.
- `LEARNING`/`RELEARNING` + `GOOD|EASY` -> `REVIEW`; `AGAIN|HARD` stays in same learning phase.

## Next-Cards Query Semantics

- Selection order tiers:
  1. Due learning cards (`state = LEARNING` and `nextReviewAt <= now`).
  2. Due review cards (`state = REVIEW` or legacy `MASTERED` with `nextReviewAt <= now`).
  3. New cards, capped by remaining account daily limit.
- Pagination:
  - Accept bounded page-size.
  - Return continuation token when additional eligible cards remain.

## Deterministic Validation Outcomes

- `RATING_INVALID`
- `CARD_NOT_FOUND`
- `DECK_NOT_FOUND`
- `AUTHORIZATION_DENIED`
- `DAILY_LIMIT_REACHED_FOR_NEW_CARDS`
- `INVALID_PAGINATION`

## Consistency and Failure Handling

- Scheduling update in Aurora is the primary transactional outcome of rating.
- Async DynamoDB write is eventually consistent and non-transactional relative to Aurora update.
- Logging failure cannot invalidate a committed schedule update and must be surfaced via observability signals.
