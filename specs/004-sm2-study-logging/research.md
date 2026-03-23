# Phase 0 Research: FSRS v4 Spaced Repetition and Study Activity Logging

## Decision 1: Treat FSRS scheduling as a dedicated domain service
- Decision: Implement all FSRS v4 rating math and state transitions in a single Spaced Repetition domain service consumed by study APIs.
- Rationale: Centralizing scheduling logic prevents drift between endpoints and keeps algorithm fidelity testable.
- Alternatives considered: Embedding math in controller/service handlers (rejected due to duplication risk and weaker testability).

## Decision 2: Model card progression with FSRS-compatible states
- Decision: Use `NEW`, `LEARNING`, `REVIEW`, and `RELEARNING` as the active state machine, while treating legacy `MASTERED` records as `REVIEW` during retrieval and scheduling.
- Rationale: Aligns with FSRS flow and avoids unsafe bulk state rewrites of historical records.
- Alternatives considered: Hard-migrating all historical `MASTERED` values immediately (rejected due to migration risk).

## Decision 3: Enforce daily new-card limits by unique-card counting per user-day
- Decision: Daily limit checks count unique cards first studied today in server timezone and only gate introduction of additional new cards.
- Rationale: Aligns with product rule wording and avoids under- or over-counting repeated reviews of the same card.
- Alternatives considered: Counting all rating events (rejected because repeated interactions would prematurely exhaust the quota).

## Decision 4: Prioritize next-card selection by due urgency tiers
- Decision: Next-card retrieval order is due learning cards first, then due review cards, then eligible new cards.
- Rationale: Keeps learning-step continuity while honoring due review obligations before introducing additional new material.
- Alternatives considered: Random mixed ordering (rejected because it can delay urgent relearning and due reviews).

## Decision 5: Keep mutable scheduling state in Aurora and immutable activity logs in DynamoDB
- Decision: Use Aurora for authoritative card-learning state and quota reads/writes; write append-only rating activity records to DynamoDB `StudyActivityLog`.
- Rationale: Transactional scheduling updates belong in relational data; high-write event logging is better suited to DynamoDB patterns.
- Alternatives considered: Storing everything in Aurora (rejected due to event-log scaling and access-pattern mismatch); storing schedule state in DynamoDB (rejected due to complex transactional consistency requirements).

## Decision 6: Execute activity log writes asynchronously and non-blocking
- Decision: Trigger DynamoDB put operations asynchronously after successful rating persistence, returning API response without waiting on log completion.
- Rationale: Preserves responsive study UX and satisfies the non-blocking logging requirement.
- Alternatives considered: Synchronous write-before-response (rejected because DynamoDB latency/failures would degrade user-facing API responsiveness).

## Decision 7: Handle async logging failures as observable, non-fatal events
- Decision: A logging failure does not roll back the successful scheduling update; it emits structured failure telemetry for operational follow-up.
- Rationale: Protects primary user outcome (learning progression) while still maintaining auditability via observability.
- Alternatives considered: Failing the rating request when log write fails (rejected because availability of analytics storage should not block studying).

## Decision 8: Contract-level validation for rating values and time-spent inputs
- Decision: Rating endpoint accepts only `AGAIN`, `HARD`, `GOOD`, `EASY` and validates time-spent bounds before applying scheduling updates.
- Rationale: Input validation protects scheduling correctness and keeps downstream logging data quality consistent.
- Alternatives considered: Lenient coercion/defaulting of invalid rating values (rejected because it creates hidden scheduling errors).

## Decision 9: Apply pagination contract to next-cards list response
- Decision: Define bounded page size and continuation-token behavior for next-cards responses.
- Rationale: Satisfies constitutional pagination requirements for list-producing APIs and prevents oversized payloads.
- Alternatives considered: Unbounded list return (rejected due to governance violation and scalability concerns).

## Decision 10: Align observability with runtime paths and constitutional telemetry standards
- Decision: Emit success/failure telemetry for due-card fetch, rating processing, and async DynamoDB write outcomes, with alarm hooks in CloudWatch/New Relic.
- Rationale: Supports production diagnosability and compliance with mandated observability standards.
- Alternatives considered: Logging only hard failures (rejected because partial success states and async failure paths would be invisible).
