# Data Model: SM-2 Spaced Repetition and Study Activity Logging

## Relational Entities (Aurora MySQL)

### 1. CardLearningState (existing entity, feature extension)
- Purpose: Authoritative per-user scheduling state for each card.
- Key fields:
  - `id` (UUID, PK)
  - `userId` (UUID, not null)
  - `cardId` (UUID, not null)
  - `state` (ENUM: `NEW`, `LEARNING`, `MASTERED`, `REVIEW`, not null)
  - `intervalDays` (DOUBLE, not null, default 0)
  - `easeFactor` (DOUBLE, not null, default 2.5)
  - `lastReviewedAt` (TIMESTAMP, nullable)
  - `nextReviewAt` (TIMESTAMP, nullable)
  - `learningStepGoodCount` (INT, not null, default 0)
  - `createdAt` (TIMESTAMP, not null)
  - `updatedAt` (TIMESTAMP, not null)
- Validation rules:
  - `easeFactor >= 1.3` always.
  - `intervalDays >= 0` always.
  - `state = NEW` implies no completed study action yet for that user/card pair.
  - `learningStepGoodCount` increments for qualifying learning-step `GOOD` ratings and resets on `AGAIN`.
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
  - `newInterval` (number, required)
  - `newEaseFactor` (number, required)
- Validation rules:
  - `timeSpentMs >= 0`.
  - `newEaseFactor >= 1.3`.
  - Immutable after write.

## State Transition Rules

### Card state transitions on rating
- `NEW` + `GOOD` (first learning step) -> `LEARNING`; `nextReviewAt = now + 10 minutes`; `learningStepGoodCount = 1`.
- `LEARNING` + `GOOD` when `learningStepGoodCount = 1` -> `MASTERED` waiting; `intervalDays = 1`; `nextReviewAt = now + 1 day`.
- `MASTERED` becomes review-eligible once `nextReviewAt <= now`; treated as due `REVIEW` card for selection.
- `AGAIN` on non-new active learning/review state -> `LEARNING`; immediate relearning (`intervalDays = 0`); `easeFactor = max(1.3, easeFactor - 0.2)`.
- `HARD` -> `intervalDays = intervalDays * 1.2`; `easeFactor = max(1.3, easeFactor - 0.15)`.
- `GOOD` (review behavior) -> `intervalDays = intervalDays * easeFactor`.
- `EASY` -> `intervalDays = intervalDays * easeFactor * 1.3`; `easeFactor = max(1.3, easeFactor + 0.15)`.

## Next-Cards Query Semantics

- Selection order tiers:
  1. Due learning cards (`state = LEARNING` and `nextReviewAt <= now`).
  2. Due review cards (`state = REVIEW` or `MASTERED` with `nextReviewAt <= now`).
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
