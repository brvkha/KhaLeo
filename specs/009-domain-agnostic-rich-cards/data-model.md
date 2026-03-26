# Data Model: Domain-Agnostic Rich Card Content and Study UI Layout

## Relational Entities (Aurora MySQL)

### 1. Card (extended existing entity)

- Purpose: Canonical flashcard content record used by create/edit and study rendering.
- Key fields:
  - `id` (UUID, PK)
  - `deck_id` (UUID, FK)
  - `term` (VARCHAR/TEXT, not null)
  - `answer` (VARCHAR/TEXT, not null)
  - `image_url` (VARCHAR, nullable)
  - `part_of_speech` (VARCHAR, nullable)
  - `phonetic` (VARCHAR, nullable)
  - `examples_json` (JSON/TEXT array, not null default `[]`)
  - `version` (BIGINT or INT, not null; optimistic concurrency token)
  - `created_at`, `updated_at` (timestamps)
- Validation rules:
  - `term`: required plain text
  - `answer`: required plain text
  - `image_url`: null or absolute HTTPS URL with allowlisted host
  - `part_of_speech`: nullable plain text
  - `phonetic`: nullable plain text
  - `examples_json`: array of 0..20 strings; each item trimmed non-empty and <= 300 chars
  - Full payload size (create/update): <= 64KB

### 2. CardMigrationAudit (optional operational artifact)

- Purpose: Optional migration-run evidence for rollout visibility when needed in prod/staging pipelines.
- Key fields (if implemented):
  - `migration_name`
  - `executed_at`
  - `rows_touched`
- Validation rules:
  - Must not alter runtime contract; used only for operational traceability.

## API View Models

### CardWriteRequest (create/update)

- Fields:
  - `term: string`
  - `answer: string`
  - `imageUrl: string | null`
  - `partOfSpeech: string | null`
  - `phonetic: string | null`
  - `examples: string[]`
  - `version: number` (required on update)
- Rules:
  - Update is full-object replace (V1)
  - Update rejects stale version with `409 Conflict`

### CardReadResponse

- Fields:
  - Mirrors write model plus `id`, `deckId`, `version`, `createdAt`, `updatedAt`
- Rules:
  - Null metadata lines (`partOfSpeech`, `phonetic`) are omitted in UI rendering (preview/study)

### StudyCardViewModel

- Purpose: Study-rendered projection of card content.
- Regions:
  - `coreContent`: `term`, `image`, `answer`, `phonetic`, `partOfSpeech`
  - `examplesContent`: list used in dedicated scroll container
- Rules:
  - `examplesContent` scroll state resets on face flip
  - Rating controls visible only on back face

## State Transitions

### 1. Study Card Face/UI State

- `FRONT` -> `BACK` on flip action
- `BACK` -> `FRONT` on next-card transition
- Transition side effects:
  - entering `BACK`: rating controls become visible
  - any face transition: examples scroll offset resets to 0

### 2. Scheduling State

- No new scheduling-state transitions introduced by this feature.
- Existing backend FSRS v6 transitions remain source of truth.

## Migration Strategy

### Additive schema/data migration

- Add columns/structure needed for rich fields.
- Backfill existing rows:
  - `image_url = null`
  - `examples_json = []`
  - `part_of_speech = null` if absent
  - `phonetic = null` if absent
- Constraints:
  - Migration must be idempotent.
  - Rollback remains simple because change is additive/default-based.

## Deterministic Validation Outcomes

- `CARD_VALIDATION_TERM_REQUIRED`
- `CARD_VALIDATION_ANSWER_REQUIRED`
- `CARD_VALIDATION_IMAGE_URL_INVALID`
- `CARD_VALIDATION_IMAGE_HOST_NOT_ALLOWED`
- `CARD_VALIDATION_EXAMPLES_LIMIT_EXCEEDED`
- `CARD_VALIDATION_EXAMPLE_EMPTY`
- `CARD_VALIDATION_EXAMPLE_TOO_LONG`
- `CARD_VALIDATION_PAYLOAD_TOO_LARGE`
- `CARD_VERSION_CONFLICT`
