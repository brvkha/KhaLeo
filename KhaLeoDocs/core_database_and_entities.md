# SPEC 001: Core Database Schema and JPA Entities

## 1. Context & Objective
This specification defines the foundational data layer for the "Kha Leo Flashcard" application. It covers the creation of Relational Database schemas (AWS Aurora MySQL) managed via Flyway, the corresponding Spring Data JPA Entities, and the NoSQL schema design (AWS DynamoDB) for tracking study activity logs.

## 2. Technical Constraints (Referencing Constitution)
- **Database:** MySQL 8.0 dialect for Aurora.
- **Migration:** Flyway is strictly required for all DDL/DML changes. No `spring.jpa.hibernate.ddl-auto=update` in production.
- **JPA Annotations:** Use Lombok (`@Getter`, `@Setter`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`). **Do not use `@Data`** on JPA entities to avoid `hashCode()` and `toString()` performance issues with lazy loading.
- **Primary Keys:** Use `UUID` generation strategy for all relational entities to ensure global uniqueness and security (preventing ID enumeration).
- **Auditing:** All entities must implement JPA Auditing (`@CreatedDate`, `@LastModifiedDate`).

## 3. Relational Database Models (Aurora MySQL)

### 3.1. Entity: `User`
Represents the system users (Admin, User) and guests.
- `id` (UUID, PK)
- `email` (String, Unique, Not Null)
- `passwordHash` (String, Not Null)
- `role` (Enum: `ROLE_USER`, `ROLE_ADMIN`, default: `ROLE_USER`)
- `isEmailVerified` (Boolean, default: false)
- `dailyLearningLimit` (Integer, default: 9999) - Account-level limit for Spaced Repetition.
- `createdAt` (Timestamp), `updatedAt` (Timestamp)

### 3.2. Entity: `Deck`
Represents a collection of flashcards created by a User.
- `id` (UUID, PK)
- `author` (ManyToOne -> User, Not Null)
- `name` (String, Not Null, max length 100)
- `description` (Text)
- `coverImageUrl` (String, nullable) - Stores the S3 Presigned URL/Path.
- `tags` (String, comma-separated or separate table, depending on optimal search performance)
- `isPublic` (Boolean, default: false)
- `createdAt` (Timestamp), `updatedAt` (Timestamp)

### 3.3. Entity: `Card`
Represents a single flashcard within a Deck.
- `id` (UUID, PK)
- `deck` (ManyToOne -> Deck, Not Null)
- `frontText` (Text, nullable if media exists)
- `frontMediaUrl` (String, nullable) - S3 URL for Image/Audio.
- `backText` (Text, nullable if media exists)
- `backMediaUrl` (String, nullable) - S3 URL for Image/Audio.
- `createdAt` (Timestamp), `updatedAt` (Timestamp)

### 3.4. Entity: `CardLearningState` (FSRS Tracking)
Tracks the learning progress of a specific Card for a specific User.
- `id` (UUID, PK)
- `card` (ManyToOne -> Card, Not Null)
- `user` (ManyToOne -> User, Not Null)
- `state` (Enum: `NEW`, `LEARNING`, `RELEARNING`, `REVIEW`, `MASTERED`, default: `NEW`)
- `intervalInDays` (Integer, default: 0)
- `nextReviewDate` (Timestamp, nullable)
- `lastReviewedAt` (Timestamp, nullable)
- `learningStepGoodCount` (Integer, default: 0)
- `fsrsStability` (Decimal, default: 0)
- `fsrsDifficulty` (Decimal, default: 0)
- `fsrsElapsedDays` (Integer, default: 0)
- `fsrsScheduledDays` (Integer, default: 0)
- `fsrsReps` (Integer, default: 0)
- `fsrsLapses` (Integer, default: 0)

## 4. NoSQL Schema (AWS DynamoDB)

### 4.1. Table: `StudyActivityLog`
Stores high-throughput, immutable logs of every card review action.
- `logId` (String/UUID, Partition Key)
- `timestamp` (String/ISO-8601, Sort Key)
- `userId` (String/UUID, GSI Hash Key for querying logs by user)
- `cardId` (String/UUID)
- `ratingGiven` (Enum/String: `AGAIN`, `HARD`, `GOOD`, `EASY`)
- `timeSpentMs` (Integer) - Time spent thinking before rating.

## 5. Acceptance Criteria
1. Flyway migration script `V1__init_schema.sql` is generated accurately reflecting the entities.
2. Spring Boot application starts successfully without JPA validation errors.
3. DynamoDB table definition (via AWS SDK v2 `@DynamoDbBean` or Terraform equivalent model) is properly documented in code.
4. JPA relationships (OneToMany, ManyToOne) are configured with `FetchType.LAZY` to optimize queries.

## 6. Execution Instructions for AI
Proceed with generating the Flyway scripts and Java Entity classes based on these constraints. Ensure proper package structuring (`com.khaleo.flashcard.entity`, `com.khaleo.flashcard.model.dynamo`).