# Research: Domain-Agnostic Rich Card Content and Study UI Layout

## Decision 1: Use domain-agnostic card schema with nullable metadata

- Decision: Keep canonical payload as `term`, `answer`, optional `imageUrl`, optional `partOfSpeech`, optional `phonetic`, and `examples: string[]`.
- Rationale: Works across language and non-language subjects while avoiding early over-modeling; supports current use cases without introducing language-specific hard-coding.
- Alternatives considered:
  - Separate schemas per language/domain: rejected for V1 due complexity and fragmentation.
  - Keep only front/back with freeform notes blob: rejected due weak validation and inconsistent UX rendering.

## Decision 2: Keep external image URL model with HTTPS + allowlist

- Decision: Allow external images only when URL is absolute HTTPS and host is in configurable allowlist.
- Rationale: Preserves simple authoring flow while reducing mixed-content and untrusted-host risk.
- Alternatives considered:
  - Allow any HTTP/HTTPS URL: rejected due security and content-quality risk.
  - Backend proxy/scan all images first: deferred due larger infra/runtime scope.
  - Disable rendering external images in V1: rejected because media display is core requirement.

## Decision 3: Full-object replace for update API

- Decision: Use full-payload create and full-object replace update semantics in V1.
- Rationale: Keeps FE/BE validation path deterministic, especially for bounded examples array and nullable fields.
- Alternatives considered:
  - Partial PATCH updates: rejected in V1 due merge ambiguity and higher contract/test complexity.
  - Mixed create-full/update-partial: rejected due inconsistent client behavior.

## Decision 4: Concurrency protection via optimistic token

- Decision: Require update concurrency token (`version` or equivalent timestamp token), return `409 Conflict` on stale token.
- Rationale: Prevents silent overwrite when multiple clients edit same card.
- Alternatives considered:
  - Last-write-wins: rejected due data-loss risk.
  - Pessimistic locking: rejected due higher contention and implementation overhead.

## Decision 5: Plain-text rendering policy for V1 content fields

- Decision: Treat `term`, `answer`, and `examples` as plain text; escape HTML in render path.
- Rationale: Minimizes XSS/rendering complexity while preserving predictable display on web/mobile.
- Alternatives considered:
  - Markdown subset: deferred to future formatting feature.
  - Sanitized HTML: rejected in V1 due parser/sanitizer complexity and security maintenance burden.

## Decision 6: Bounded payload and examples constraints

- Decision: Enforce max 20 examples, max 300 chars per example, and max 64KB create/update payload.
- Rationale: Preserves responsive UI/API behavior and protects against accidental oversized payloads.
- Alternatives considered:
  - No payload cap in V1: rejected due abuse/perf risk.
  - Larger cap (256KB): rejected as unnecessary for defined feature scope.

## Decision 7: Migration strategy through idempotent Flyway script

- Decision: Deliver additive Flyway migration that backfills legacy cards with `imageUrl = null` and `examples = []`.
- Rationale: Matches existing backend schema-evolution standard and supports deterministic rollback-lite behavior.
- Alternatives considered:
  - Runtime lazy migration on read: rejected due inconsistent behavior and observability gaps.
  - Manual one-off SQL runbook: rejected due operational drift risk.

## Decision 8: Study layout behavior keeps algorithm path unchanged

- Decision: Implement UI-only changes for card rendering/layout while preserving existing backend FSRS v6 scheduling behavior.
- Rationale: Reduces algorithm regression risk and isolates scope to content model + presentation updates.
- Alternatives considered:
  - Coupled UI + scheduler refactor: rejected because out of scope and high regression risk.

## Integration Notes

- Backend integration points:
  - Card create/update request validation layer.
  - Flyway migration for additive columns/default data.
  - Existing study rating endpoint remains contract-compatible for scheduling path.
- Frontend integration points:
  - Card create/edit form and preview rendering.
  - Study card component with split core-content and examples-scroll regions.
  - Mobile-specific fixed rating control bar with safe-area support.
- Observability expectations:
  - Structured validation failure logs for allowlist URL, payload over-limit, and stale update token.
  - Existing rating telemetry remains unchanged because scheduling behavior is intentionally preserved.
