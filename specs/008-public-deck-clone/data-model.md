# Data Model: Public Deck Discovery, Private Workspace, and Re-import Merge

## Relational Entities (Aurora MySQL)

### 1. Deck (existing entity, feature-constrained behavior)
- Purpose: Represents both public source decks and learner-owned private decks.
- Key fields:
  - `id` (UUID, PK)
  - `ownerId` (UUID, not null)
  - `visibility` (ENUM: `PUBLIC`, `PRIVATE`, not null)
  - `name` (string, not null)
  - `description` (string, nullable)
  - `tags` (collection/string payload)
  - `coverMediaRef` (string, nullable)
  - `createdAt`, `updatedAt` (timestamps)
- Validation rules:
  - Public discovery includes only `visibility=PUBLIC`.
  - Study/cards workspace includes only `visibility=PRIVATE` and `ownerId=actingUser`.

### 2. Card (existing entity, feature-constrained behavior)
- Purpose: Represents flashcard content in a deck.
- Key fields:
  - `id` (UUID, PK)
  - `deckId` (UUID, FK -> Deck)
  - `frontText` (string, not null)
  - `backText` (string, not null)
  - `vocabulary` (string/normalized search field)
  - `mediaRefList` (array/list of media keys)
  - `createdAt`, `updatedAt` (timestamps)
- Validation rules:
  - Private CRUD/search applies only when parent deck is learner-owned private deck.

### 3. DeckImportLink (new entity)
- Purpose: Tracks lineage between source public deck and learner private copy.
- Key fields:
  - `id` (UUID, PK)
  - `sourceDeckId` (UUID, FK -> Deck, must reference `PUBLIC` deck)
  - `targetPrivateDeckId` (UUID, FK -> Deck, must reference learner-owned `PRIVATE` deck)
  - `importedByUserId` (UUID, not null)
  - `lastImportedAt` (timestamp, not null)
  - `lastMergeStatus` (ENUM: `SUCCESS`, `CONFLICT_REQUIRED`, `FAILED`)
- Validation rules:
  - (`sourceDeckId`, `targetPrivateDeckId`) unique pair.
  - Source deck cannot equal target deck.

### 4. ReimportMergeConflict (new entity)
- Purpose: Captures unresolved conflict units produced during re-import merge.
- Key fields:
  - `id` (UUID, PK)
  - `deckImportLinkId` (UUID, FK -> DeckImportLink)
  - `conflictScope` (ENUM: `DECK_FIELD`, `CARD_ITEM`, `MEDIA_REF`)
  - `targetEntityId` (UUID/string, identifies conflicted deck/card element)
  - `fieldPath` (string, e.g., `name`, `card.frontText`, `mediaRef[2]`)
  - `localValueSnapshot` (JSON/text)
  - `cloudValueSnapshot` (JSON/text)
  - `resolutionChoice` (ENUM: `LOCAL`, `CLOUD`, nullable until decision)
  - `resolvedAt` (timestamp, nullable)
- Validation rules:
  - Merge finalization requires all active conflicts to have `resolutionChoice`.

### 5. CardSchedulingState (existing entity, retained rules)
- Purpose: Per-learner schedule state for spaced repetition.
- Key fields (existing):
  - `userId`, `cardId`, `state`, `interval`, `easeFactor`, `nextReviewAt`, `lastReviewedAt`
- Validation rules:
  - New-card rating timing mapping must hold:
    - `Again=+1 minute`
    - `Hard=+6 minutes`
    - `Good=+10 minutes`
    - `Easy=+1 day`
  - Non-new card transitions preserve constitutional Anki-style behavior and minimum ease-factor constraints.

## Relationships

- `Deck` 1:N `Card`
- `DeckImportLink.sourceDeckId` -> `Deck(id)` where source visibility is public.
- `DeckImportLink.targetPrivateDeckId` -> `Deck(id)` where target visibility is private and owned by importing user.
- `DeckImportLink` 1:N `ReimportMergeConflict`.
- `CardSchedulingState` links learner-card progression independent of source deck ownership.

## State and Merge Transition Notes

### Import/Re-import state machine
- `IMPORT_REQUESTED` -> `COPYING` -> `SUCCESS`
- `REIMPORT_REQUESTED` -> `MERGING`
  - if no conflict: `MERGING` -> `SUCCESS`
  - if conflict: `MERGING` -> `CONFLICT_REQUIRED` -> (`RESOLVING`) -> `SUCCESS`
  - on unrecoverable error: `FAILED`

### Conflict-resolution semantics
- No-conflict units: preserve local edits where unchanged by source updates.
- Conflict units: user must choose `LOCAL` or `CLOUD` before merge completion.
- Merge operation is deterministic once all conflict decisions are recorded.

## Deterministic Validation Outcomes

- `DISCOVERY_IMPORT_AUTH_REQUIRED`
- `DECK_NOT_FOUND`
- `DECK_NOT_PUBLIC`
- `PRIVATE_DECK_OWNERSHIP_REQUIRED`
- `IMPORT_LINK_NOT_FOUND`
- `MERGE_CONFLICTS_PENDING`
- `INVALID_CONFLICT_RESOLUTION_CHOICE`
- `INVALID_PAGINATION`
