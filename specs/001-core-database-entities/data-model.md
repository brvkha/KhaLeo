# Data Model: Core Database and Entity Foundation

## Relational Entities (Aurora MySQL)

### 1. User
- Purpose: Account identity, authorization profile, and account-level learning policy.
- Fields:
  - `id` (UUID, PK)
  - `email` (VARCHAR, unique, not null)
  - `passwordHash` (VARCHAR, not null)
  - `role` (ENUM: `ROLE_USER`, `ROLE_ADMIN`; default `ROLE_USER`)
  - `isEmailVerified` (BOOLEAN, default `false`)
  - `dailyLearningLimit` (INT, default `9999`, valid range `1..9999`)
  - `createdAt` (TIMESTAMP, not null)
  - `updatedAt` (TIMESTAMP, not null)
- Validation rules:
  - Email must be unique, non-empty, trimmed, and lowercase-normalized.
  - `dailyLearningLimit` must be between 1 and 9999.
- Relationships:
  - One-to-many with `Deck`.
  - One-to-many with `CardLearningState`.

### 2. Deck
- Purpose: User-owned card collection and visibility metadata.
- Fields:
  - `id` (UUID, PK)
  - `authorId` (UUID, FK -> User.id, not null)
  - `name` (VARCHAR(100), not null)
  - `description` (TEXT, nullable)
  - `coverImageUrl` (VARCHAR, nullable)
  - `tags` (TEXT or normalized association, implementation decision in tasks)
  - `isPublic` (BOOLEAN, default `false`)
  - `createdAt` (TIMESTAMP, not null)
  - `updatedAt` (TIMESTAMP, not null)
- Validation rules:
  - `name` required, max 100 chars.
- Relationships:
  - Many-to-one with `User`.
  - One-to-many with `Card`.

### 3. Card
- Purpose: Study item with front/back prompt and answer content.
- Fields:
  - `id` (UUID, PK)
  - `deckId` (UUID, FK -> Deck.id, not null)
  - `frontText` (TEXT, nullable)
  - `frontMediaUrl` (VARCHAR, nullable)
  - `backText` (TEXT, nullable)
  - `backMediaUrl` (VARCHAR, nullable)
  - `createdAt` (TIMESTAMP, not null)
  - `updatedAt` (TIMESTAMP, not null)
- Validation rules:
  - Front side must have `frontText` or `frontMediaUrl`.
  - Back side must have `backText` or `backMediaUrl`.
- Relationships:
  - Many-to-one with `Deck`.
  - One-to-many with `CardLearningState`.

### 4. CardLearningState
- Purpose: Per-user FSRS v4 progression state for each card.
- Fields:
  - `id` (UUID, PK)
  - `cardId` (UUID, FK -> Card.id, not null)
  - `userId` (UUID, FK -> User.id, not null)
  - `state` (ENUM: `NEW`, `LEARNING`, `REVIEW`, `RELEARNING`; default `NEW`, with legacy `MASTERED` compatibility)
  - `fsrsStability` (DECIMAL/FLOAT, default `0`)
  - `fsrsDifficulty` (DECIMAL/FLOAT, default `0`)
  - `fsrsScheduledDays` (INT, default `0`)
  - `fsrsElapsedDays` (INT, default `0`)
  - `fsrsReps` (INT, default `0`)
  - `fsrsLapses` (INT, default `0`)
  - `nextReviewDate` (TIMESTAMP, nullable)
  - `version` (BIGINT, not null, optimistic lock)
  - `createdAt` (TIMESTAMP, not null)
  - `updatedAt` (TIMESTAMP, not null)
- Validation rules:
  - Exactly one active row per `(userId, cardId)` enforced by unique constraint.
  - Scheduled and elapsed day counters cannot be negative.
  - Difficulty is clamped to [1,10] once initialized.
- Relationships:
  - Many-to-one with `Card`.
  - Many-to-one with `User`.
- Concurrency rule:
  - Updates use optimistic locking with one bounded retry; persistent conflict is rejected deterministically.
- State transitions:
  - `NEW -> LEARNING` for `AGAIN|HARD|GOOD` and `NEW -> REVIEW` for `EASY`.
  - `LEARNING -> REVIEW` when promoted by qualifying ratings.
  - `REVIEW -> RELEARNING` on `AGAIN` lapse.
  - `RELEARNING -> REVIEW` when promoted by qualifying ratings.

## NoSQL Entity (DynamoDB)

### 5. StudyActivityLog
- Purpose: Immutable high-throughput log of review actions.
- Primary key:
  - Partition key: `logId` (String UUID)
  - Sort key: `timestamp` (ISO-8601 string)
- Access pattern keys:
  - GSI partition key: `userId` for querying a user's activity timeline.
- Attributes:
  - `userId` (String UUID)
  - `cardId` (String UUID)
  - `ratingGiven` (`AGAIN`, `HARD`, `GOOD`, `EASY`)
  - `timeSpentMs` (Integer)
  - `writeStatus` (optional operational metadata for retry/dead-letter observability)
- Validation rules:
  - `ratingGiven` must be valid enum value.
  - `timeSpentMs` must be >= 0.
  - Event records are append-only.

## Cross-Store Consistency Rules

- Aurora commit is source of truth for learning progress and scheduling.
- DynamoDB log writes are asynchronous and best-effort with retry + dead-letter.
- Failure to write activity log does not roll back successful Aurora transaction.
- Observability events must correlate Aurora commit and DynamoDB publish attempts.

## Deterministic Validation Outcomes

- Persistence validation failures map to canonical error codes:
  - `DUPLICATE_EMAIL`
  - `INVALID_DAILY_LEARNING_LIMIT`
  - `INVALID_CARD_CONTENT`
  - `MISSING_RELATIONSHIP`
  - `OPTIMISTIC_LOCK_CONFLICT`
  - `VALIDATION_REJECTED`
