# Phase 0 Research: Core Database and Entity Foundation

## Decision 1: Use Flyway-only schema migration for Aurora MySQL
- Decision: All relational schema changes are delivered through Flyway versioned migrations; Hibernate auto-DDL remains disabled outside local experimentation.
- Rationale: Guarantees deterministic schema evolution across environments and satisfies constitution data-governance requirements.
- Alternatives considered: Hibernate `ddl-auto=update` (rejected due to non-deterministic drift); manual SQL execution (rejected due to auditability and repeatability gaps).

## Decision 2: Treat Aurora as transactional source of truth and DynamoDB logs as asynchronous side stream
- Decision: Commit transactional entities to Aurora first; publish StudyActivityLog asynchronously with retry and dead-letter handling.
- Rationale: Preserves correctness for user progress and FSRS v4 scheduling when DynamoDB is transiently unavailable.
- Alternatives considered: Synchronous dual-write all-or-nothing (rejected due to higher availability risk); log-first ordering (rejected because it can produce orphaned events).

## Decision 3: Enforce one active learning-state row per `(userId, cardId)`
- Decision: Use a uniqueness constraint for active state identity and update-in-place semantics for subsequent reviews.
- Rationale: Prevents conflicting schedule truth and simplifies FSRS v4 correctness.
- Alternatives considered: Timestamp-based latest resolution across duplicate rows (rejected due to ambiguity); immutable history without current-state pointer (rejected as unnecessary complexity in this phase).

## Decision 4: Concurrency control for learning-state updates
- Decision: Apply optimistic locking via version field with one bounded retry on conflict.
- Rationale: Protects consistency under concurrent review submissions while avoiding heavy lock contention.
- Alternatives considered: Last-write-wins (rejected due to silent loss risk); pessimistic locking for all writes (rejected due to throughput/latency overhead for expected concurrency profile).

## Decision 5: Card content validation rule
- Decision: Front side requires text or media; back side requires text or media.
- Rationale: Ensures every card remains reviewable and prevents unusable empty cards.
- Alternatives considered: Any single-side content accepted (rejected due to invalid study experiences); text-only requirement (rejected due to media-first cards).

## Decision 6: Daily learning limit policy
- Decision: `dailyLearningLimit` default is 9999; valid range is 1..9999.
- Rationale: Supports effectively unrestricted onboarding while preserving explicit account-level governance.
- Alternatives considered: Null/unlimited semantics (rejected due to ambiguity and validation complexity); smaller defaults (rejected for unnecessary early friction).

## Decision 7: Observability requirements for persistence layer
- Decision: Emit structured failure/success telemetry for migration execution, relational writes, async DynamoDB publish, retry attempts, dead-letter events, and optimistic-lock conflicts.
- Rationale: Required by constitution and necessary for diagnosing dual-store data path incidents.
- Alternatives considered: Minimal error logging only (rejected due to weak operability and slower incident triage).

## Decision 8: Testing strategy for foundational persistence
- Decision: Use unit tests for validation logic, integration tests with real database containers for schema/constraints, and targeted concurrency tests for optimistic locking; include contract tests for migration and entity-schema compatibility.
- Rationale: Matches constitutional quality gate and verifies behavior where failures are most likely.
- Alternatives considered: Unit-only approach (rejected due to inability to validate real constraints); full E2E-only approach (rejected as slow and less diagnostic for data-layer defects).

## Decision 9: New Relic instrumentation hook strategy
- Decision: Add a dedicated instrumentation component that emits structured New Relic-compatible events for relational success/failure and async publish/dead-letter outcomes.
- Rationale: Centralizes observability extension points without coupling persistence services to a vendor SDK in this phase.
- Alternatives considered: Direct New Relic agent API calls from each service (rejected due to tight vendor coupling); no instrumentation adapter (rejected due to weak operational traceability).

## Decision 10: CloudWatch alarm baseline for persistence reliability
- Decision: Define Terraform-managed CloudWatch alarms for persistence write error rate and async retry/dead-letter spikes.
- Rationale: Provides actionable reliability guardrails for data path regressions and async degradation.
- Alternatives considered: Dashboard-only monitoring (rejected due to delayed incident response); manual alarm setup (rejected by IaC governance).

## Phase 6 Verification Notes
- Full test execution attempted in Phase 6 validation.
- Integration tests currently fail in local run due MySQL connectivity/bootstrap constraints in the execution environment.
- Coverage artifact recorded under `backend/build/reports/tests/phase6-test-and-coverage-summary.md` to preserve evidence and unblock governance traceability.
