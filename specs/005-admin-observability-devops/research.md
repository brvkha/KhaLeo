# Phase 0 Research: Administration, Observability, and DevOps Pipeline

## Decision 1: Enforce banned-user access on every authenticated request using stateful checks
- Decision: Enforce ban status at request time by checking current user state from Aurora during authenticated request processing, and deny requests immediately even for previously issued tokens.
- Rationale: Stateless JWT claims cannot reflect post-issuance ban changes; request-time checks are required to satisfy immediate-block semantics.
- Alternatives considered: Ban-only-at-login (rejected because existing tokens continue to work); token blacklists only (rejected due to invalidation complexity and drift risk).

## Decision 2: Use admin role authorization at endpoint and method levels
- Decision: Protect all admin endpoints under /api/v1/admin/** with admin role checks and method-level authorization guards.
- Rationale: Centralized role checks reduce accidental exposure and align with constitution security controls.
- Alternatives considered: Service-only role checks (rejected due to inconsistent controller-layer enforcement).

## Decision 3: Keep platform stats near-real-time with bounded lag
- Decision: Provide platform stats with up to 5 minutes freshness lag rather than strict read-after-write consistency.
- Rationale: Meets clarified requirement while reducing expensive aggregate recomputation under load.
- Alternatives considered: Strict real-time queries on every request (rejected due to performance cost); eventual consistency with no bound (rejected due to operational ambiguity).

## Decision 4: Ship JSON logs to Splunk HEC through async appender chain
- Decision: Extend logback configuration with a Splunk HEC appender wrapped by AsyncAppender, while retaining JSON console output.
- Rationale: Asynchronous shipping prevents log transport latency from blocking request threads and preserves existing structured logging behavior.
- Alternatives considered: Synchronous HEC writes (rejected due to request-latency impact); Splunk-only output with no local JSON stream (rejected due to local diagnosability and fallback loss).

## Decision 5: Enable New Relic via javaagent at runtime, not feature-specific code hooks
- Decision: Enable New Relic using JVM startup javaagent configuration and keep code-level instrumentation additive only where already established.
- Rationale: Satisfies APM requirement with minimal code churn and lower integration risk.
- Alternatives considered: Custom in-app APM implementation (rejected due to maintenance overhead and weaker platform support).

## Decision 6: Publish immutable commit-SHA artifacts to S3 for deterministic deploy/rollback
- Decision: Build backend jar in GitHub Actions and upload immutable versioned artifacts keyed by commit SHA.
- Rationale: Immutable artifacts provide traceability, reproducibility, and deterministic rollback behavior.
- Alternatives considered: Mutable latest app.jar key only (rejected due to rollback ambiguity and weak auditability).

## Decision 7: Deploy to EC2 through AWS SSM send-command with full-target dispatch
- Decision: Use SSM send-command targeting tagged EC2 instances, dispatch to all intended targets, then fail workflow if any target fails.
- Rationale: Meets clarified deployment behavior and avoids direct SSH requirements in private subnet topology.
- Alternatives considered: SSH-based deployment (rejected by requirement); fail-fast on first target (rejected because full rollout state visibility is required).

## Decision 8: Keep infrastructure drift control exclusively in Terraform
- Decision: Represent all alarm/config resource changes in infra/terraform and avoid manual console-only changes.
- Rationale: Preserves constitutional governance and reproducible infrastructure state.
- Alternatives considered: Console-created alarms for speed (rejected due to drift and governance violation).
