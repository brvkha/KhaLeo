# Feature Specification: Core Database and Entity Foundation

**Feature Branch**: `001-core-database-entities`  
**Created**: 2026-03-13  
**Status**: Draft  
**Input**: User description: "SPEC 001: Core Database Schema and JPA Entities"

## Clarifications

### Session 2026-03-13

- Q: What consistency policy should apply when one persistence target is unavailable? -> A: Prioritize relational commit as source of truth; write activity logs asynchronously with retry and dead-letter handling.
- Q: How should active learning state uniqueness be enforced per user-card pair? -> A: Enforce one active record per (userId, cardId) and update that record in place.
- Q: What default and valid range should apply to account daily learning limits? -> A: Default to 9999, with allowed values from 1 to 9999.
- Q: What card-content validity rule should apply to front/back fields? -> A: Each side must contain content: front requires text or media, and back requires text or media.
- Q: What concurrency strategy should apply to concurrent learning-state updates? -> A: Use optimistic concurrency with version checks and one bounded retry on conflict.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Persist Learning Domain Data (Priority: P1)

As a learner, I need my account, decks, cards, and card progress to be stored
consistently so my study history and next reviews are preserved across sessions.

**Why this priority**: Without reliable persistence, no core learning workflow can
function or retain value.

**Independent Test**: Create a user, a deck, cards, and learning-state records;
restart the application and verify all records remain valid and linked.

**Acceptance Scenarios**:

1. **Given** a new user account, **When** the user creates a deck and cards,
   **Then** all created records are stored with unique identifiers and can be
   retrieved with relationships intact.
2. **Given** an existing user-card pair, **When** the learning state is updated,
   **Then** the state, ease factor, interval, and next review metadata are
   persisted for that exact pair without duplicating conflicting active records.

---

### User Story 2 - Capture Study Activity History (Priority: P2)

As a learner and product owner, I need every card review action captured as an
immutable activity event so progress analysis and behavioral insights are
available without impacting core transactional data.

**Why this priority**: Study activity history is essential for analytics,
operability, and auditability of learning behavior.

**Independent Test**: Submit multiple review events for one user and verify each
event is stored as a separate immutable log record that can be queried by user.

**Acceptance Scenarios**:

1. **Given** a completed card review action, **When** the event is recorded,
   **Then** an immutable activity log entry is stored with timestamp, rating,
   user reference, card reference, and time spent.

---

### User Story 3 - Enforce Data Integrity Rules (Priority: P3)

As a platform maintainer, I need strict data integrity and audit timestamps so
invalid records are rejected early and change history remains traceable.

**Why this priority**: Data quality issues in foundational models propagate into
every future feature and become expensive to correct.

**Independent Test**: Attempt to write invalid records (missing required fields,
duplicate unique values, invalid relationships) and verify the system rejects
them while valid records include created/updated audit metadata.

**Acceptance Scenarios**:

1. **Given** a record missing mandatory attributes, **When** persistence is
  attempted, **Then** the write is rejected with a clear validation failure.

---

### Edge Cases

- User attempts to create two accounts with the same email address.
- Card is created with both text and media fields empty on the front or back
  side.
- Learning state update is attempted for a card-user association that does not
  exist.
- A second active learning-state record is attempted for the same user and card
  pair.
- Daily learning limit is set outside the allowed range of 1 to 9999.
- Activity event arrives with a malformed timestamp or unknown rating value.
- If relational persistence succeeds but activity-log persistence is unavailable,
  commit relational data and enqueue activity-log delivery retries with
  dead-letter fallback.
- Two review submissions for the same `(userId, cardId)` arrive concurrently and
  attempt to update the same active learning-state record.

### Constitutional Impact *(mandatory)*

- **Algorithm Fidelity**: Direct impact. The feature defines persistence for SM-2
  progression metadata and card states (New, Learning, Mastered (waiting),
  Review), and stores account-level daily learning limits.
- **Security Impact**: Indirect impact. The feature defines sensitive identity and
  credential-related data boundaries and must enforce uniqueness, integrity, and
  auditable timestamps for user records.
- **Observability Impact**: Direct impact. Persistence failures, validation
  failures, and state-transition writes must emit structured operational signals
  so data-layer reliability can be monitored.
- **Infrastructure Impact**: Direct impact. The feature requires relational
  schema resources and a study-activity log store definition aligned with
  infrastructure-as-code delivery.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST provide persistent records for users, decks, cards, and
  per-user card-learning state with globally unique identifiers.
- **FR-002**: System MUST enforce unique email addresses per account and reject
  duplicates.
- **FR-003**: System MUST maintain required relationships between user, deck,
  card, and learning-state records so orphaned records are not accepted.
- **FR-004**: System MUST record and maintain learning-state attributes required
  for SM-2 progression, including state, ease factor, interval, and next review
  date.
- **FR-005**: System MUST support card states New, Learning, Mastered (waiting),
  and Review as valid domain states.
- **FR-006**: System MUST store account-level daily learning limits and apply a
  default value of 9999 when no custom limit is provided; accepted values MUST
  be within 1 to 9999.
- **FR-007**: System MUST store immutable study activity events that include user
  reference, card reference, timestamp, rating, and time spent.
- **FR-008**: System MUST automatically maintain creation and last-update audit
  timestamps for all relational entities.
- **FR-009**: System MUST enforce required field validation for all core entities
  and reject records that violate nullability, range, or relationship rules.
- **FR-010**: Every list-producing API introduced by this feature MUST define
  pagination behavior; if no list endpoint is delivered in scope, the
  specification MUST explicitly mark pagination as not applicable.
- **FR-011**: System MUST define observability outputs for persistence success and
  failure paths affecting foundational entities and activity logs.
- **FR-012**: Schema evolution for relational data MUST be versioned and
  repeatable so new environments can reconstruct the same structure reliably.
- **FR-013**: Relational data persistence MUST be treated as the source-of-truth
  commit path; activity-log persistence MUST be asynchronous and MUST implement
  retry with dead-letter handling when the log store is unavailable.
- **FR-014**: System MUST enforce a unique active learning-state record for each
  `(userId, cardId)` pair and MUST update that same record for subsequent
  reviews instead of creating additional active rows.
- **FR-015**: System MUST reject card creation or update if front side lacks both
  text and media, or back side lacks both text and media.
- **FR-016**: Learning-state updates MUST use optimistic concurrency control on
  the active `(userId, cardId)` record with version checks, and MUST perform at
  most one retry when a version conflict is detected.

### Key Entities *(include if feature involves data)*

- **User**: Represents an account with identity and policy attributes,
  including email uniqueness, verification status, role, and daily learning
  limit.
- **Deck**: Represents a user-authored collection of cards with ownership,
  visibility, and descriptive metadata.
- **Card**: Represents a study item with front/back content that may include
  text and media references, where each side must include at least one content
  type.
- **CardLearningState**: Represents per-user progress for a specific card,
  including SM-2 progression values and next review timing, with exactly one
  active record per `(userId, cardId)` pair.
- **StudyActivityLog**: Represents immutable review events used for analytics,
  troubleshooting, and learning-behavior tracking.

## Assumptions

- Guest usage may exist in the product, but this feature models only registered
  account data required for persistent learning behavior.
- The account daily learning limit is always an integer in the range 1 to 9999,
  with default value 9999.
- Activity logs are append-only and are not edited after creation.
- Pagination is marked not applicable for this feature because it defines data
  models and persistence behavior, not user-facing list endpoints.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of required core entities (User, Deck, Card,
  CardLearningState, StudyActivityLog) can be created and read back with valid
  relationships in acceptance testing.
- **SC-002**: 100% of tested invalid writes for required-field, uniqueness, and
  relationship rules are rejected with deterministic validation outcomes.
- **SC-003**: 100% of card-learning state transitions in acceptance scenarios
  persist state and scheduling metadata without data loss after service restart.
- **SC-004**: At least 95% of sampled study activity events submitted during
  test runs are queryable by user within 5 seconds of submission.
- **SC-005**: Quality gates are met for this feature: no unresolved requirement
  ambiguities, explicit constitutional impact coverage, and automated-test
  evidence supporting foundational persistence behavior.
- **SC-006**: In fault-injection tests where the activity-log store is
  unavailable, 100% of valid relational writes remain committed and failed log
  events are captured for retry or dead-letter processing.
- **SC-007**: 100% of attempts to create a duplicate active learning-state row
  for an existing `(userId, cardId)` pair are rejected, and valid updates modify
  the existing row in place.
- **SC-008**: 100% of tested account daily learning limit writes outside 1 to
  9999 are rejected, and accounts without explicit value persist default 9999.
- **SC-009**: 100% of tested card writes with empty front side or empty back
  side are rejected, while valid cards with content on both sides are accepted.
- **SC-010**: In concurrency tests with simultaneous updates for the same
  `(userId, cardId)` pair, no duplicate active rows are created and final state
  remains consistent after one bounded retry strategy.
