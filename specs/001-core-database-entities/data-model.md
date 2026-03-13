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
  - Email must be unique and non-empty.
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
- Purpose: Per-user SM-2 progression state for each card.
- Fields:
  - `id` (UUID, PK)
  - `cardId` (UUID, FK -> Card.id, not null)
  - `userId` (UUID, FK -> User.id, not null)
  - `state` (ENUM: `NEW`, `LEARNING`, `MASTERED`, `REVIEW`; default `NEW`)
  - `easeFactor` (DECIMAL/FLOAT, default `2.5`)
  - `intervalInDays` (INT, default `0`)
  - `nextReviewDate` (TIMESTAMP, nullable)
  - `version` (BIGINT, not null, optimistic lock)
  - `createdAt` (TIMESTAMP, not null)
  - `updatedAt` (TIMESTAMP, not null)
- Validation rules:
  - Exactly one active row per `(userId, cardId)` enforced by unique constraint.
  - Interval cannot be negative.
  - `easeFactor` must stay above minimum SM-2 threshold enforced by domain logic.
- Relationships:
  - Many-to-one with `Card`.
  - Many-to-one with `User`.
- State transitions:
  - `NEW -> LEARNING` on first study.
  - `LEARNING -> REVIEW` when interval scheduling begins.
  - `REVIEW -> MASTERED` when long-interval threshold is reached.
  - `MASTERED -> REVIEW` when review is due and scheduled again.

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
