# SPEC 004: SM-2 Spaced Repetition Engine and Study Activity Logging

## 1. Context & Objective
This is the heart of the "Kha Leo Flashcard" app. It implements the Spaced Repetition logic (based on the SM-2 algorithm), manages the state machine of flashcards during a study session, enforces account-level daily learning limits, and asynchronously logs all study activities to AWS DynamoDB.

## 2. Technical Constraints
- **Algorithm:** Standard Anki SM-2.
- **State Machine:** Cards transition through `NEW` -> `LEARNING` -> `MASTERED` (waiting for review) -> `REVIEW`.
- **DynamoDB Integration:** Use AWS SDK v2 Enhanced Client (`software.amazon.awssdk:dynamodb-enhanced`). Writes to DynamoDB MUST be asynchronous (e.g., using `@Async` or `CompletableFuture`) so the study API responds instantly to the user.

## 3. The SM-2 Engine & Daily Limits
- **Account-Level Daily Limit:** Check the `User.dailyLearningLimit`. The system must count how many *unique* cards the user has studied `today` (server timezone). If the limit is reached, do not serve new cards.
- **Learning Steps:** - 1st "Good" on a `NEW` card -> State becomes `LEARNING`, next review in 10 minutes.
  - 2nd "Good" on a `LEARNING` card -> State becomes `MASTERED` (waiting state), interval becomes 1 day, next review is tomorrow. Once the 1 day passes, it officially becomes `REVIEW`.
- **Rating Logic (Again, Hard, Good, Easy):**
  - `Again`: Resets interval to 0, drops card back to `LEARNING`, decreases Ease Factor by 0.2.
  - `Hard`: Interval * 1.2, decreases Ease Factor by 0.15.
  - `Good`: Interval * Ease Factor.
  - `Easy`: Interval * Ease Factor * 1.3, increases Ease Factor by 0.15.
  - *Constraint:* Minimum Ease Factor is 1.3.

## 4. Study Session (APIs)
- `GET /api/v1/study/decks/{deckId}/next-cards`: Fetches a batch of cards due for study. Prioritizes: 1. `LEARNING` cards whose 10-minute timer is up. 2. `REVIEW` cards that are due. 3. `NEW` cards (up to the daily limit).
- `POST /api/v1/study/cards/{cardId}/rate`: The core study action.
  - Payload: `{ "rating": "GOOD", "timeSpentMs": 4500 }`.
  - Action 1: Calculate new SM-2 interval/ease and update `CardLearningState` in MySQL.
  - Action 2: Fire an async event to log the activity to DynamoDB.

## 5. DynamoDB Activity Logging
- **Table Name:** `StudyActivityLog`.
- **Item Schema:** `logId` (UUID, PK), `timestamp` (ISO-8601, SK), `userId` (GSI Hash Key), `cardId`, `deckId`, `ratingGiven`, `timeSpentMs`, `newInterval`, `newEaseFactor`.
- **Service implementation:** Create an `ActivityLogService` that handles the async `putItem` operation to the DynamoDB table.

## 6. Execution Instructions for AI
Implement the math and logic for the SM-2 algorithm meticulously in a dedicated domain service (`SpacedRepetitionService`). Ensure thread-safe asynchronous execution for the DynamoDB logging. Create the APIs necessary for the frontend to fetch due cards and submit ratings.