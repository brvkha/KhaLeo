# Persistence Contract: Core Database and Entity Foundation

## Purpose

Define contractual expectations for persistence behavior consumed by application
services and future API layers.

## Relational Contract (Aurora + Flyway)

- Migration contract:
  - `V1__init_schema.sql` creates User, Deck, Card, CardLearningState tables.
  - No production schema changes outside Flyway versioned migration files.
- Entity contract:
  - Primary keys are UUIDs for all relational entities.
  - All entities maintain `createdAt` and `updatedAt`.
  - `User.email` is unique and non-null.
  - `User.dailyLearningLimit` default `9999`, allowed range `1..9999`.
  - `Card` requires content on both front and back sides (text or media on each).
  - `CardLearningState` supports states `NEW|LEARNING|MASTERED|REVIEW`.
  - Unique active state constraint on `(userId, cardId)`.
  - `CardLearningState.version` participates in optimistic concurrency checks.

## NoSQL Contract (DynamoDB)

- Table contract:
  - `StudyActivityLog` is append-only.
  - Key schema uses `logId` + `timestamp`.
  - `userId` index supports user timeline queries.
- Event payload contract:
  - Required fields: `logId`, `timestamp`, `userId`, `cardId`, `ratingGiven`, `timeSpentMs`.
  - `ratingGiven` is one of `AGAIN|HARD|GOOD|EASY`.
  - `timeSpentMs` must be non-negative.

## Cross-Store Reliability Contract

- Aurora write is authoritative for learning state and card progression.
- DynamoDB activity log writes are asynchronous and may lag Aurora commit.
- Failed DynamoDB writes are retried and then dead-lettered if retries exhaust.
- A failed DynamoDB write never invalidates a successful Aurora transaction.

## Compatibility Expectations

- Future schema changes must preserve backward compatibility for existing
  persisted records unless explicitly documented as a breaking migration.
- Contract changes require updates to `spec.md`, `plan.md`, and tests.
