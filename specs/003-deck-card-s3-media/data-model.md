# Data Model: Deck/Card Management and Media Uploads

## Relational Entities (Aurora MySQL)

### 1. Deck (existing, extended usage)
- Purpose: Learner-owned collection of cards with visibility and metadata.
- Fields:
  - `id` (UUID, PK)
  - `author_id` (UUID, FK -> `users.id`, not null)
  - `name` (VARCHAR(100), not null)
  - `description` (TEXT, nullable)
  - `cover_image_url` (VARCHAR(2048), nullable)
  - `tags` (TEXT, nullable)
  - `is_public` (BOOLEAN, not null, default `false`)
  - `created_at`, `updated_at` (TIMESTAMP, not null)
- Validation rules:
  - `name` required and <= 100 chars.
  - `is_public` cannot be null.
- Relationships:
  - One-to-many with `Card`.
  - Many-to-one with `User` (author).

### 2. Card (existing, extended usage)
- Purpose: Study item within a deck with front/back content and optional media references.
- Fields:
  - `id` (UUID, PK)
  - `deck_id` (UUID, FK -> `decks.id`, not null)
  - `front_text` (TEXT, nullable)
  - `front_media_url` (VARCHAR(2048), nullable)
  - `back_text` (TEXT, nullable)
  - `back_media_url` (VARCHAR(2048), nullable)
  - `created_at`, `updated_at` (TIMESTAMP, not null)
- Validation rules:
  - Front side must contain text or media.
  - Back side must contain text or media.
  - Search semantics: `front_text`/`back_text` use case-insensitive contains; vocabulary token uses case-insensitive exact match.
- Relationships:
  - Many-to-one with `Deck`.
  - One-to-many with `CardLearningState`.

### 3. CardLearningState (existing cascade dependency)
- Purpose: Learner progress state for a given card.
- Fields: existing schema from feature 001/002 remains unchanged.
- Lifecycle implication:
  - When a deck is deleted, dependent cards and related learning-state rows are removed.

### 4. MediaUploadAuthorization (new)
- Purpose: Audit and policy record for issued presigned upload grants and request-throttle outcomes.
- Fields:
  - `id` (UUID, PK)
  - `user_id` (UUID, FK -> `users.id`, not null)
  - `object_key` (VARCHAR(512), not null)
  - `content_type` (VARCHAR(64), not null)
  - `max_size_bytes` (BIGINT, not null, fixed at 5242880)
  - `expires_at` (TIMESTAMP, not null, issue time + 5 minutes)
  - `issued_at` (TIMESTAMP, not null)
  - `status` (ENUM: `ISSUED|EXPIRED|REJECTED_TYPE|REJECTED_SIZE|REJECTED_RATE_LIMIT`)
  - `rejection_reason` (VARCHAR(128), nullable)
- Validation rules:
  - `content_type` must be one of allowed set (`image/jpeg`, `image/png`, `audio/mpeg`, `audio/webm`).
  - `expires_at` must be exactly 5 minutes after issue.
  - Requests beyond 30 per user per rolling minute are rejected and logged.
- Relationships:
  - Many-to-one with `User`.

### 5. MediaObjectReference (new)
- Purpose: Reference count tracking for uploaded objects to support delete-only-when-unreferenced behavior.
- Fields:
  - `object_key` (VARCHAR(512), PK)
  - `reference_count` (INT, not null, >= 0)
  - `last_referenced_at` (TIMESTAMP, not null)
  - `last_dereferenced_at` (TIMESTAMP, nullable)
- Validation rules:
  - `reference_count` never negative.
  - Physical delete is eligible only when `reference_count = 0`.
- Relationships:
  - Logical relationship from deck cover or card media fields by shared `object_key`.

## Derived Rules

- Read-access rule:
  - Public deck cards are readable by anyone.
  - Private deck cards are readable only by deck owner or admin.
- Mutation rule:
  - Deck/card create-update-delete allowed only for owner or admin.
- Media authorization rule:
  - Authorization request must pass type/size/rate-limit checks before presigned URL issuance.
- Media lifecycle rule:
  - Media objects are deleted from storage only when no deck/card references remain.

## State Transitions

### Deck/Card Visibility Access
- `PUBLIC_READABLE` for deck/cards when `deck.is_public = true`.
- `RESTRICTED_READABLE` when `deck.is_public = false` (owner/admin only).

### Media Authorization Lifecycle
- `REQUESTED -> ISSUED` on valid request.
- `REQUESTED -> REJECTED_TYPE|REJECTED_SIZE|REJECTED_RATE_LIMIT` on validation failure.
- `ISSUED -> EXPIRED` after 5 minutes.

### Media Reference Lifecycle
- `reference_count` increments when deck/card starts referencing an `object_key`.
- `reference_count` decrements when reference removed or parent entity deleted.
- `reference_count == 0` triggers storage delete attempt.

## Non-Relational Data Impact

- No new DynamoDB entities are introduced for this feature.
- Existing learning activity logging to DynamoDB remains unchanged.

## Migration Impact

- Existing `decks` and `cards` tables are reused with current columns.
- New Flyway migration(s) are expected for `media_upload_authorization` and `media_object_reference` (or equivalent) if chosen implementation persists these records.
- Cascade behavior on deck deletion must be verified to ensure dependent cards and learning states are removed as required.

## Implementation Notes (2026-03-16)

- Implemented migration `V3__deck_card_media_schema.sql` creates both media tables with indexes and constraints.
- Deck/card create-update-delete flows now increment/decrement `media_object_references.reference_count`.
- Physical object delete is executed only when `reference_count` transitions to zero.
- `media_upload_authorizations` records are created for both issued and rejected authorization attempts.
