# Feature Specification: Domain-Agnostic Rich Card Content and Study UI Layout

**Feature Branch**: `009-domain-agnostic-rich-cards`  
**Created**: 2026-03-26  
**Status**: Draft  
**Input**: User clarification session for richer card content model, create/edit UX updates, and study-screen layout behavior while preserving current backend scheduling behavior.

## Goal

Extend flashcards beyond front/back-only content into a domain-agnostic card schema that supports optional media and structured metadata, while keeping study-state behavior unchanged under current backend FSRS v6 logic.

## Clarifications

### Session 2026-03-26

- Q: Canonical main field name for card prompt/content root? -> A: Use `term`.
- Q: Keep or rename `partOfSpeech`? -> A: Keep `partOfSpeech`.
- Q: Pinyin-specific field or generalized field? -> A: Generalize to `phonetic`.
- Q: Can `partOfSpeech` and `phonetic` be null? -> A: Yes, both nullable.
- Q: If nullable fields are null, how should UI render? -> A: Hide the line completely (do not display `null`).
- Q: Where does null-hiding apply? -> A: Apply consistently in create/edit preview and study view.
- Q: Example-item validation? -> A: Reject empty/whitespace-only values.
- Q: Max examples behavior? -> A: Limit to 20 and block adding more in UI.
- Q: Max length per example? -> A: 300 characters per item.
- Q: `imageUrl` validation? -> A: Must be valid absolute `https` URL from configured allowlist domains.
- Q: Image load failure behavior? -> A: Show placeholder with error icon.
- Q: Image sizing policy? -> A: Responsive scaling with constrained max height (200-300px range).
- Q: Study backside content order? -> A: `term(front)` -> `image` -> `answer/back` -> `phonetic + partOfSpeech` -> `examples`.
- Q: Scroll strategy for long content? -> A: Only examples section scrolls.
- Q: Examples scroll when flipping? -> A: Reset to top after flip.
- Q: Mobile bottom controls safe area? -> A: Required safe-area inset handling.
- Q: When should rating buttons show? -> A: Only after flip to back side.
- Q: Desktop/tablet rating-bar behavior? -> A: No fixed-bottom requirement on desktop/tablet.
- Q: On-screen instructional text policy? -> A: Minimal guidance only (first-use or empty-state context).
- Q: Create/edit examples input UI? -> A: Add/remove per-line item UI, not freeform textarea.
- Q: Drag-and-drop example ordering in V1? -> A: Out of scope.
- Q: API contract detail in spec? -> A: Must be explicitly defined.
- Q: Migration strategy? -> A: DB migration script.
- Q: Migration quality constraints? -> A: Idempotent required; rollback can remain simple due additive defaults.
- Q: Filtering/search by new metadata in V1? -> A: Out of scope.
- Q: Mandatory tests in V1? -> A: Migration, create/edit validation, mobile fixed rating controls, card scroll behavior, image fallback.
- Q: Future-proof schema principle? -> A: Domain-agnostic schema rule must be explicit; no language-specific fields unless abstracted.
- Q: Scheduling algorithm source of truth? -> A: Keep current backend behavior and FSRS v6; remove SM2 references from this feature scope.
- Q: External image URL policy for V1? -> A: Only `https` URLs from configurable allowlist domains are accepted.
- Q: Update contract style for card payload in V1? -> A: Use full-object replace on update (no partial patch contract in V1).
- Q: Text content rendering policy for V1? -> A: Plain text only for `term`, `answer`, and `examples`; HTML must be escaped.
- Q: Concurrent card update conflict handling in V1? -> A: Use optimistic concurrency checks and return `409 Conflict` on version mismatch.
- Q: Max create/update payload size for V1? -> A: Enforce maximum payload size of 64KB.

## User Scenarios & Testing

### User Story 1 - Author Rich Cards in Create/Edit Flow (Priority: P1)

As a card author, I can create and edit cards with `term`, `answer`, optional external `imageUrl`, optional `partOfSpeech`, optional `phonetic`, and a validated list of examples so content is flexible across subjects and languages.

**Why this priority**: Data capture is the first dependency; study UI and migration value depend on having valid rich-card content.

**Independent Test**: Can be tested by creating and editing cards in UI with valid and invalid combinations, then verifying persistence and validation behavior.

**Acceptance Scenarios**:

1. **Given** author is on card create/edit, **When** they input examples, **Then** UI allows add/remove per-item rows and blocks adding item 21.
2. **Given** an example row contains empty or whitespace-only value, **When** author saves, **Then** save is rejected with field-level validation error.
3. **Given** an example row exceeds 300 characters, **When** author saves, **Then** save is rejected with field-level validation error.
4. **Given** `imageUrl` is not a valid allowlisted absolute `https` URL, **When** author saves, **Then** save is rejected with field-level validation error.
5. **Given** `partOfSpeech` and `phonetic` are null, **When** preview is shown, **Then** those lines are omitted entirely.

---

### User Story 2 - Study Rich Cards in Focused Layout (Priority: P1)

As a learner, I can study on a large, low-noise flashcard surface where core content remains visible and only examples scroll when long, so learning remains focused on answer recall.

**Why this priority**: Study interaction is the core learner value and must remain clear on both mobile and desktop.

**Independent Test**: Can be tested by running study sessions on cards with and without media/metadata/examples and verifying visibility, scroll, and flip behavior.

**Acceptance Scenarios**:

1. **Given** card has media and metadata, **When** learner flips to back side, **Then** content order is `term -> image -> answer -> phonetic/partOfSpeech -> examples`.
2. **Given** examples overflow visible space, **When** learner reviews back side, **Then** only examples area scrolls while core section remains visible.
3. **Given** learner scrolls examples and flips card, **When** card side changes, **Then** examples scroll position resets to top.
4. **Given** `partOfSpeech` or `phonetic` is null, **When** card renders in study mode, **Then** null fields are hidden (not shown as text).
5. **Given** `imageUrl` fails to load, **When** card renders image, **Then** fallback placeholder and error icon appear.

---

### User Story 3 - Keep Rating Behavior and FSRS v6 Logic Intact (Priority: P2)

As a learner and product owner, I can continue using the existing FSRS v6 scheduling behavior with unchanged rating outcomes while the card content model evolves.

**Why this priority**: Algorithm stability avoids regressions and protects existing learning progression.

**Independent Test**: Can be tested by comparing scheduling outputs before/after feature rollout for identical rating inputs.

**Acceptance Scenarios**:

1. **Given** study session under this feature, **When** learner submits `Again/Hard/Good/Easy`, **Then** backend scheduling behavior remains exactly as current FSRS v6 implementation.
2. **Given** mobile device viewport, **When** learner flips to back side, **Then** rating buttons appear fixed bottom-center with safe-area support.
3. **Given** desktop/tablet viewport, **When** learner flips to back side, **Then** rating controls render in natural layout and are not forced fixed-bottom.

## Edge Cases

- Existing legacy cards without rich fields must remain renderable.
- Legacy data with missing newly added columns must be migrated to defaults (`imageUrl=null`, `examples=[]`).
- Invalid URL schemes (`ftp`, `file`, malformed URLs) are rejected on save.
- Extremely long single-word examples without spaces should still wrap or clip safely without layout break.
- Image URL reachable but returns non-image content type.
- Example list exactly at 20 items allows edit/delete but blocks additional add.
- First-time user guidance should not block or overlap rating controls.

## Requirements

### Functional Requirements

- **FR-001**: System MUST support card schema with fields: `term`, `answer`, optional `imageUrl`, optional `partOfSpeech`, optional `phonetic`, and `examples` (array of strings).
- **FR-002**: System MUST enforce `examples` maximum length of 20 items.
- **FR-003**: System MUST enforce each example item as non-empty, non-whitespace, and maximum 300 characters.
- **FR-004**: System MUST enforce `imageUrl` validation as absolute `https` URL whose host matches a configurable allowlist domain set.
- **FR-005**: Create/Edit UI MUST provide per-item add/remove controls for examples and MUST block add when item count reaches 20.
- **FR-006**: Create/Edit preview MUST hide `partOfSpeech` and/or `phonetic` rows when values are null.
- **FR-007**: Study UI MUST display `imageUrl` content on both front and back sides when present.
- **FR-008**: Study UI MUST display placeholder + error icon when image loading fails.
- **FR-009**: Study back-side content MUST follow this order: `term`, `image`, `answer`, `phonetic + partOfSpeech`, `examples`.
- **FR-010**: Study UI MUST keep core content visible and allow scrolling only inside examples section when overflow occurs.
- **FR-011**: Study UI MUST reset examples scroll position on card flip.
- **FR-012**: Rating buttons (`Again/Hard/Good/Easy`) MUST appear only after card flips to back side.
- **FR-013**: Mobile study view MUST render rating controls fixed at bottom-center and MUST respect safe-area insets.
- **FR-014**: Desktop/tablet study view MUST NOT require fixed-bottom rating controls.
- **FR-015**: Study screen MUST remove unnecessary instructional text and keep guidance minimal (first-use and empty-state only).
- **FR-016**: System MUST preserve existing backend scheduling and study-state behavior under FSRS v6 with no algorithmic change introduced by this feature.
- **FR-017**: Backend and frontend contracts for this feature MUST avoid language-specific specialization and remain domain-agnostic.
- **FR-018**: System MUST provide DB migration script that backfills legacy cards as `imageUrl = null` and `examples = []`.
- **FR-019**: Migration script MUST be idempotent.
- **FR-020**: V1 scope MUST exclude metadata-based filtering/search and drag-and-drop reordering of examples.
- **FR-021**: System MUST expose configuration for image-host allowlist and MUST reject create/update payloads with non-allowlisted hosts.
- **FR-022**: Card update API in V1 MUST use full-object replace semantics where client sends complete card payload (`term`, `answer`, `imageUrl`, `partOfSpeech`, `phonetic`, `examples`).
- **FR-023**: System MUST treat `term`, `answer`, and `examples` as plain-text fields in V1 and MUST escape/neutralize HTML markup during render.
- **FR-024**: Card update API MUST enforce optimistic concurrency using version metadata (or equivalent `updatedAt` token) and MUST return `409 Conflict` when client version is stale.
- **FR-025**: Card create/update API MUST reject request payloads larger than 64KB with a clear validation/error outcome.

### API Contract (V1)

#### Card Payload Shape

```json
{
  "term": "string",
  "answer": "string",
  "imageUrl": "https://example.com/image.jpg",
  "partOfSpeech": "noun",
  "phonetic": "\u02c8ek.s\u00e6m.p\u0259l",
  "examples": [
    "Example sentence 1",
    "Example sentence 2"
  ]
}
```

#### Validation Rules

- `term`: required string.
- `answer`: required string.
- `imageUrl`: nullable; if present must be absolute `https` URL and host must belong to configured allowlist.
- `partOfSpeech`: nullable string.
- `phonetic`: nullable string.
- `examples`: required array (can be empty) with max 20 items.
- `examples[i]`: required non-empty non-whitespace string, max 300 chars.
- `term`, `answer`, `examples[i]`: plain-text-only rendering in V1; HTML is escaped.
- Request payload size for create/update card: max 64KB.

#### Backward Compatibility

- Existing records missing rich fields are interpreted after migration as:
  - `imageUrl = null`
  - `examples = []`
  - `partOfSpeech = null` (if absent)
  - `phonetic = null` (if absent)

#### Update Semantics (V1)

- Create uses full payload submission.
- Update uses full-object replace semantics (no partial patch behavior in V1).
- Missing optional fields in update payload are treated as explicit null/empty values according to schema rules.
- Update requests must include concurrency token (`version` or equivalent). On mismatch, server rejects with `409 Conflict` and current persisted snapshot metadata.

### Key Entities

- **RichCardContent**: Canonical card content with domain-agnostic fields (`term`, `answer`, optional media/metadata, examples array).
- **StudyCardViewModel**: Read model for study rendering that separates core content region and scrollable examples region.
- **CardContentMigrationUnit**: Migration artifact that upgrades legacy front/back cards to new default-compatible shape.

## Non-Functional Requirements

- **NFR-001**: Mobile study layout must remain usable from width >= 360px with bottom rating controls visible and tappable.
- **NFR-002**: Image rendering errors must fail gracefully without breaking rating interaction.
- **NFR-003**: Examples scroll interactions must not degrade card flip responsiveness under normal load.
- **NFR-004**: No regression allowed in existing FSRS v6 rating-to-schedule latency targets.

## Assumptions

- Current backend FSRS v6 engine is already deployed and considered source of truth.
- Existing create/edit and study routes remain the same; this feature evolves schema and UI behavior.
- Additive DB migration can be delivered safely without complex rollback scripts.

## Success Criteria

- **SC-001**: 100% of create/edit submissions violating example or URL rules are rejected with field-level validation feedback.
- **SC-002**: 100% of cards with null `partOfSpeech`/`phonetic` hide those rows in preview and study views.
- **SC-003**: 100% of mobile study sessions display back-side rating controls fixed bottom-center with safe-area handling.
- **SC-004**: 100% of image load failures show fallback placeholder + error icon and preserve study flow continuity.
- **SC-005**: 100% of migrated legacy cards are readable with defaults (`imageUrl=null`, `examples=[]`) after migration.
- **SC-006**: 100% of regression tests confirm unchanged FSRS v6 scheduling outputs for identical rating inputs.

## Mandatory Test Coverage (V1)

- Migration test for legacy card backfill defaults.
- Create/Edit validation tests for examples and image URL.
- Study mobile layout test for fixed bottom rating controls + safe-area inset.
- Study behavior test for examples-only scroll and scroll reset on flip.
- Study rendering test for image fallback placeholder/icon on load error.
