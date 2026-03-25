# SPEC 004: FSRS v6 Study Engine and Study Activity Logging

## 1. Context & Objective
This is the heart of the "Kha Leo Flashcard" app. It implements the Spaced Repetition logic based on FSRS v6, manages flashcard state transitions during study sessions, enforces account-level daily learning limits, and asynchronously logs study activities to DynamoDB.

## 2. Technical Constraints
- **Algorithm:** FSRS v6 (17-weight vector, retrievability-based scheduling).
- **State Machine:** Cards transition through `NEW` -> `LEARNING`/`REVIEW` and `REVIEW` -> `RELEARNING` on lapse (`MASTERED` remains legacy-compatible when present).
- **DynamoDB Integration:** Use AWS SDK v2 Enhanced Client (`software.amazon.awssdk:dynamodb-enhanced`). Writes to DynamoDB MUST be asynchronous (e.g., using `@Async` or `CompletableFuture`) so the study API responds instantly to the user.

## 3. The FSRS v6 Engine & Daily Limits
- **Account-Level Daily Limit:** Check the `User.dailyLearningLimit`. The system must count how many *unique* cards the user has studied `today` (server timezone). If the limit is reached, do not serve new cards.
- **First-step behavior for new cards:**
  - `Again` -> 1 minute
  - `Hard` -> 6 minutes
  - `Good` -> 10 minutes
  - `Easy` -> 1 day
- **Review/Relearning behavior:**
  - Use FSRS retrievability and stability/difficulty updates for `AGAIN|HARD|GOOD|EASY`.
  - Persist `fsrsStability`, `fsrsDifficulty`, `fsrsElapsedDays`, `fsrsScheduledDays`, `fsrsReps`, `fsrsLapses`.

## 4. Study Session (APIs)
- `GET /api/v1/study-session/decks/{deckId}/next-cards`: Fetches a batch of cards due for study. Prioritizes: 1. due `LEARNING/RELEARNING`, 2. due `REVIEW/MASTERED`, 3. `NEW` cards (up to daily limit).
- `POST /api/v1/study-session/cards/{cardId}/rate`: The core study action.
  - Payload: `{ "rating": "GOOD", "timeSpentMs": 4500 }`.
  - Action 1: Apply FSRS outcome and update `CardLearningState` in MySQL.
  - Action 2: Fire an async event to log the activity to DynamoDB.

## 5. DynamoDB Activity Logging
- **Table Name:** `StudyActivityLog`.
- **Item Schema:** `logId` (UUID, PK), `timestamp` (ISO-8601, SK), `userId` (GSI Hash Key), `cardId`, `deckId`, `ratingGiven`, `timeSpentMs`, `newInterval`, `newStability`, `newDifficulty`.
- **Service implementation:** Create an `ActivityLogService` that handles the async `putItem` operation to the DynamoDB table.

## 6. Execution Instructions for AI
Implement the FSRS v6 scheduling logic in a dedicated domain service (`SpacedRepetitionService`). Ensure thread-safe asynchronous execution for DynamoDB logging. Keep study APIs under `/api/v1/study-session/**` with pagination (`size`, `continuationToken`) for next-cards.