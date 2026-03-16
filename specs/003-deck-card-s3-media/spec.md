# Feature Specification: Deck/Card Management and Media Uploads

**Feature Branch**: `003-deck-card-s3-media`  
**Created**: 2026-03-16  
**Status**: Draft  
**Input**: User description from `KhaLeoDocs/deck_card_and_s3_media.md`

## Clarifications

### Session 2026-03-16

- Q: What is card read access for public decks? → A: Anyone can read cards in public decks; private deck cards are readable only by owner/admin.
- Q: What is the upload URL expiration window? → A: 5 minutes.
- Q: How should deck card search matching work? → A: Exact vocabulary is case-insensitive exact; front/back search is case-insensitive contains.
- Q: How should uploaded media be deleted? → A: Delete only when no deck/card references remain.
- Q: What is the media authorization request rate limit? → A: 30 requests per user per minute.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Manage Decks Securely (Priority: P1)

A signed-in learner can create, view, update, and delete decks, with controls that ensure only authorized users can modify deck content while public decks remain discoverable.

**Why this priority**: Deck ownership and visibility are the foundation for organizing study content and controlling access for every downstream card workflow.

**Independent Test**: Can be fully tested by creating a deck, listing decks with ownership/public filtering, editing metadata, and confirming unauthorized users cannot update or delete another user's deck.

**Acceptance Scenarios**:

1. **Given** a signed-in learner provides valid deck metadata, **When** they create a deck, **Then** the deck is saved with the learner as owner and the chosen visibility setting.
2. **Given** multiple decks with mixed ownership and visibility, **When** a user lists decks with filters, **Then** results match requested visibility/owner criteria and are returned in paginated form.
3. **Given** a deck owned by User A, **When** User B who is neither owner nor admin attempts update or delete, **Then** the request is denied and the deck remains unchanged.
4. **Given** a public deck with cards, **When** an unauthenticated user reads card content for that deck, **Then** access is allowed, while private deck cards remain restricted to owner or admin.

---

### User Story 2 - Build and Find Cards in a Deck (Priority: P2)

A deck owner can add, edit, delete, and search cards within a deck to keep learning content current and quickly locate the right flashcards.

**Why this priority**: Cards carry the core learning material, and targeted search is required to keep larger decks usable.

**Independent Test**: Can be fully tested by adding cards to a deck, updating card content, searching by front/back text and exact vocabulary in that same deck, and deleting selected cards.

**Acceptance Scenarios**:

1. **Given** a deck owned by the acting user, **When** they add a valid card, **Then** the card is associated with that deck and appears in deck-level card listings.
2. **Given** a deck containing multiple cards, **When** the user searches within the deck by front text, back text, or exact vocabulary, **Then** front/back matching uses case-insensitive contains and vocabulary uses case-insensitive exact match, with only matching cards from that deck returned in paginated form.
3. **Given** an existing card in a deck, **When** the owner updates or deletes it, **Then** the change is persisted and reflected in subsequent reads.

---

### User Story 3 - Attach Media Through Direct Upload Authorization (Priority: P3)

A user can request short-lived upload authorization for allowed media files, upload directly to storage, and reference uploaded media in deck cover or card content without routing file binaries through the application server.

**Why this priority**: Media-rich flashcards increase learning quality, while direct upload authorization reduces backend load and improves scalability.

**Independent Test**: Can be fully tested by requesting upload authorization for valid/invalid file metadata, uploading a valid file through the provided authorization, and saving the returned media reference on card or deck data.

**Acceptance Scenarios**:

1. **Given** a user requests media upload authorization with an allowed media type and size under limit, **When** the request is validated, **Then** the system returns an upload URL valid for 5 minutes and a storage object key.
2. **Given** a user requests authorization for disallowed media type or oversized file metadata, **When** validation runs, **Then** authorization is rejected with a clear validation reason.
3. **Given** a user has uploaded media and obtained a storage key, **When** they create or update a card or deck cover referencing that key, **Then** the media reference is stored and retrievable with the content.

### Edge Cases

- A user requests deck or card pages with out-of-range pagination parameters.
- A private deck is requested by a non-owner user.
- A deck delete request targets a deck that has cards and learning progress records tied to it.
- A card search request includes empty search criteria or criteria that produce no matches.
- Media authorization is requested with mismatched file extension and declared media type.
- Media upload authorization is used after expiration.
- A deck or card deletion removes one media reference while other references to the same media object still exist.
- A user exceeds media upload authorization request limits within one minute.

### Constitutional Impact *(mandatory)*

- **Algorithm Fidelity**: No change to SM-2 scheduling math or card-state transition rules. User-initiated deck deletion removes related learning-state records by design, but does not alter scheduling logic for remaining records.
- **Security Impact**: Introduces ownership/admin authorization checks for deck/card mutations, enforces direct-upload authorization for media, restricts accepted media type and size, and adds per-user request throttling on media authorization.
- **Observability Impact**: Requires audit-friendly logs and operational metrics for deck/card create-update-delete, authorization denials, media upload authorization issuance, validation failures, rate-limit rejections, and expired authorization usage.
- **Infrastructure Impact**: Uses object storage upload authorization capability and associated access policy controls; no change to deployment topology is required.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST allow signed-in users to create decks with name, description, tags, and visibility.
- **FR-002**: System MUST allow users to view deck details and card content based on visibility and access rules: anyone can read cards in public decks, while private deck cards are readable only by the owner or an admin.
- **FR-003**: System MUST allow deck owners and admins to update deck metadata and MUST deny updates from unauthorized users.
- **FR-004**: System MUST allow deck owners and admins to delete decks and MUST remove all cards and learning-state records associated with the deleted deck.
- **FR-005**: System MUST allow deck owners and admins to create, update, and delete cards in their decks and MUST deny these actions for unauthorized users.
- **FR-006**: System MUST provide deck-scoped card search where front text and back text use case-insensitive contains matching and vocabulary uses case-insensitive exact matching.
- **FR-007**: System MUST provide upload authorization for media files that returns a storage object key and an upload URL valid for 5 minutes when validation passes.
- **FR-008**: System MUST reject media upload authorization requests that exceed size limits or use unsupported media formats.
- **FR-009**: System MUST allow deck and card create/update operations to store media references supplied after successful direct upload.
- **FR-010**: System MUST delete an uploaded media object only when no remaining deck or card references to that object exist.
- **FR-011**: System MUST limit media upload authorization requests to 30 requests per user per minute and reject excess requests.
- **FR-012**: Every list-producing API MUST return paginated responses and document accepted pagination parameters and default behavior.
- **FR-013**: System MUST define required observability outputs for deck/card management paths and media upload authorization paths, including success, validation failure, authorization failure, and rate-limit rejection outcomes.
- **FR-014**: System MUST preserve existing SM-2 scheduling fidelity, card-state transitions, and account-level daily learning limits without introducing behavioral changes.

### Key Entities *(include if feature involves data)*

- **Deck**: Represents a learner-owned flashcard collection with metadata (name, description, tags), visibility, and owner identity.
- **Flashcard**: Represents a study item within a deck, including front/back text, optional media references, and searchable vocabulary content.
- **Media Upload Authorization**: Represents a short-lived permission grant for a specific media upload request, including object key, allowed media type, and expiration.
- **Learning State Record**: Represents learner progress linked to cards that may be removed when the parent deck is deleted.

### Assumptions

- Signed-in identity and role information are available to enforce owner/admin authorization checks.
- Existing media delivery paths can serve uploaded files after upload completion.
- Users provide accurate file metadata when requesting media upload authorization.

### Dependencies

- Object storage and upload-authorization capability for direct client uploads.
- Persistent storage support for deck, card, media reference, and learning-state relationships.
- Existing authentication/authorization foundation for owner/admin access control.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: At least 95% of valid deck create, update, and delete requests complete successfully on first attempt.
- **SC-002**: 100% of unauthorized deck/card mutation attempts are denied and leave stored content unchanged.
- **SC-003**: At least 95% of card searches in decks with up to 10,000 cards return first-page results within 2 seconds.
- **SC-004**: 100% of upload authorization requests for unsupported media types or oversize files are rejected before any upload is permitted.
- **SC-005**: At least 90% of users can attach media to a card or deck cover in under 2 minutes from authorization request to saved reference.
- **SC-006**: Support tickets related to deck/card access-control errors decrease by at least 25% within one release cycle after rollout.
