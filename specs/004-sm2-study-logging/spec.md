# Feature Specification: FSRS v6 Study Engine and Activity Logging

**Feature Branch**: `004-sm2-study-logging`  
**Created**: 2026-03-16  
**Status**: Draft  
**Input**: User description: "Implement FSRS v6 spaced repetition engine and DynamoDB study activity logging"

## User Scenarios & Testing *(mandatory)*

<!--
  IMPORTANT: User stories should be PRIORITIZED as user journeys ordered by importance.
  Each user story/journey must be INDEPENDENTLY TESTABLE - meaning if you implement just ONE of them,
  you should still have a viable MVP (Minimum Viable Product) that delivers value.
  
  Assign priorities (P1, P2, P3, etc.) to each story, where P1 is the most critical.
  Think of each story as a standalone slice of functionality that can be:
  - Developed independently
  - Tested independently
  - Deployed independently
  - Demonstrated to users independently
-->

### User Story 1 - Study Due Cards Reliably (Priority: P1)

As a learner, I can fetch the next cards that are currently due so I can run a focused study session without manual card selection.

**Why this priority**: Retrieving due cards is the entry point for every study session. Without it, rating and progress tracking cannot happen.

**Independent Test**: Can be fully tested by requesting next cards for a deck with mixed states and confirming ordering, due filtering, and daily new-card limit behavior.

**Acceptance Scenarios**:

1. **Given** a deck has due learning cards, due review cards, and eligible new cards, **When** the learner requests next cards, **Then** the response prioritizes due learning cards first, then due review cards, then new cards.
2. **Given** the learner has already studied the maximum number of unique new cards for today, **When** next cards are requested, **Then** no additional new cards are included in the response.
3. **Given** no cards are currently due and daily new-card quota is exhausted, **When** next cards are requested, **Then** the response returns an empty list with a clear no-cards-due outcome.

---

### User Story 2 - Rate Cards With Correct Scheduling (Priority: P2)

As a learner, I can submit a rating for a studied card so the system updates card state, due date, difficulty (D), and stability (S) according to FSRS v6 rules.

**Why this priority**: Accurate schedule updates are the core value of spaced repetition and directly determine learning effectiveness.

**Independent Test**: Can be fully tested by rating cards in each lifecycle state (`NEW`, `LEARNING`, `REVIEW`, `RELEARNING`) with each rating option (`AGAIN`, `HARD`, `GOOD`, `EASY`) and verifying resulting scheduled days, difficulty, stability, and next due date.

**Acceptance Scenarios**:

1. **Given** a `NEW` card, **When** the learner rates it `EASY`, **Then** the card transitions to `REVIEW`, initializes FSRS D/S values, and receives a due date derived from calculated scheduled days.
2. **Given** a `REVIEW` card, **When** the learner rates it `GOOD`, **Then** the system computes retrievability using elapsed days and updates stability with the FSRS recall formula.
3. **Given** a `REVIEW` card, **When** the learner rates it `AGAIN`, **Then** the card transitions to `RELEARNING`, lapses increments by 1, and stability is recalculated with the FSRS forget formula.

---

### User Story 3 - Record Study Activity Without Slowing Study (Priority: P3)

As a learner and product owner, I need each rating action recorded in an activity log for analytics and auditability without making the study action feel slow.

**Why this priority**: Logging is critical for analytics and observability, but the learner experience must remain responsive.

**Independent Test**: Can be fully tested by submitting rating actions and confirming immediate study response while corresponding activity records are created asynchronously.

**Acceptance Scenarios**:

1. **Given** a valid card rating request, **When** the learner submits the rating, **Then** the schedule update response is returned immediately and activity logging proceeds asynchronously.
2. **Given** the activity log store is temporarily unavailable, **When** a rating is submitted, **Then** the scheduling update still succeeds and the logging failure is observable for operations follow-up.

---

[Add more user stories as needed, each with an assigned priority]

### Edge Cases

<!--
  ACTION REQUIRED: The content in this section represents placeholders.
  Fill them out with the right edge cases.
-->

- A learner submits an invalid rating value not in the allowed set.
- A learner attempts to rate a card that is not part of their accessible deck.
- A card transitions from mastered-waiting to due review status exactly at the day boundary.
- Multiple rating submissions for the same card are received in rapid succession.
- Time spent is missing, zero, or unreasonably large.
- Daily quota counting must treat repeated reviews of the same card on the same day as one unique card for quota purposes.

### Assumptions

- "Today" is evaluated in the server timezone for quota and due-date calculations.
- The next-cards endpoint uses bounded pagination with default and maximum page sizes and a continuation token when additional eligible cards remain.
- Existing historical `MASTERED` records (if present) are treated as `REVIEW` for retrieval and scheduling compatibility.
- Learners are authenticated and can only study cards in decks they are authorized to access.

### Constitutional Impact *(mandatory)*

- **Algorithm Fidelity**: High impact. This feature defines FSRS v6 rating math with D/S/R memory modeling and formalizes transitions (`NEW` -> `LEARNING`/`REVIEW`, `REVIEW` -> `RELEARNING` on lapse) including account-level daily learning limits.
- **Security Impact**: Moderate impact. Study retrieval and rating must enforce authenticated access and ownership checks so users cannot study or modify other users' cards.
- **Observability Impact**: High impact. The feature requires success/failure telemetry for next-card retrieval, rating actions, and asynchronous activity logging outcomes, including error visibility when logging fails.
- **Infrastructure Impact**: Moderate impact. Requires a persistent activity-log data store and corresponding operational monitoring for asynchronous write health.

## Requirements *(mandatory)*

<!--
  ACTION REQUIRED: The content in this section represents placeholders.
  Fill them out with the right functional requirements.
-->

### Functional Requirements

- **FR-001**: System MUST provide a next-cards capability for a deck that returns only cards currently eligible for study.
- **FR-002**: System MUST prioritize returned cards in this order: due learning cards, due review cards, then eligible new cards.
- **FR-003**: System MUST enforce each account's daily learning limit based on the count of unique cards first studied that day and MUST exclude additional new cards once the limit is reached.
- **FR-004**: System MUST support card rating submissions with allowed rating values `AGAIN`, `HARD`, `GOOD`, and `EASY`.
- **FR-005**: System MUST update scheduled days, difficulty, stability, next due timestamp, and learning state according to defined FSRS v6 rules for each rating outcome.
- **FR-006**: System MUST implement FSRS v6 default parameters (`W[0..16]`, `DECAY=-0.5`, `FACTOR=0.9`) and permit parameter override for future optimization.
- **FR-007**: System MUST compute retrievability for review cards using $R(t,S)=\left(1+\frac{t}{9S}\right)^{-1}$ and use it in both recall and forget stability update formulas.
- **FR-008**: System MUST reject rating operations for inaccessible or nonexistent cards with clear error outcomes.
- **FR-009**: System MUST record each accepted rating action in an activity log containing a unique log identifier, timestamp, user identifier, card identifier, deck identifier, rating, time spent, resulting scheduled days, resulting stability, and resulting difficulty.
- **FR-010**: System MUST perform activity-log writes asynchronously so rating responses are not blocked by log-store write latency.
- **FR-011**: System MUST emit observable outcomes for next-card retrieval, rating updates, asynchronous log successes, and asynchronous log failures.
- **FR-012**: For the list-producing next-cards capability, system MUST define pagination behavior via page-size limits and a continuation token so large due sets can be retrieved predictably.
- **FR-013**: System MUST preserve schedule-update integrity even if asynchronous logging fails, and MUST surface logging failures for operational follow-up.

### Key Entities *(include if feature involves data)*

- **Card Learning State**: Represents a learner-specific scheduling record for a card, including current state, stability, difficulty, elapsed/scheduled days, repetition counters, last review timestamp, and next due timestamp.
- **Study Rating Event**: Represents a learner action on a card at study time, including selected rating and time spent.
- **Daily Learning Quota Counter**: Represents the per-user, per-day count of unique newly studied cards used to enforce new-card serving limits.
- **Study Activity Log Entry**: Represents an immutable analytic/audit record of a completed rating action and its resulting schedule values.

## Success Criteria *(mandatory)*

<!--
  ACTION REQUIRED: Define measurable success criteria.
  These must be technology-agnostic and measurable.
-->

### Measurable Outcomes

- **SC-001**: 99% of next-cards requests return only cards that are currently due or new-card eligible according to configured daily limits.
- **SC-002**: 100% of accepted rating actions produce schedule updates consistent with defined FSRS v6 formulas and state-transition rules in automated verification scenarios.
- **SC-003**: 95% of rating requests complete with a user-visible response in under 1 second under normal operating load.
- **SC-004**: 99% of successful rating actions produce a corresponding activity-log record within 30 seconds.
- **SC-005**: Operations can identify and investigate 100% of asynchronous logging failures through emitted telemetry and alertable signals.
