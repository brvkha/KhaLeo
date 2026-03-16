# Phase 0 Research: Deck/Card Management and Media Uploads

## Decision 1: Extend existing relational persistence service for deck/card CRUD
- Decision: Build deck/card update, delete, read, and list behavior in `RelationalPersistenceService` and companion controllers, reusing repository + exception-mapper patterns already in place.
- Rationale: Existing create flows for deck/card already live in persistence service with deterministic validation and observability conventions.
- Alternatives considered: Create an entirely new service stack for deck/card domain (rejected due to duplication and inconsistent error/telemetry behavior).

## Decision 2: Enforce owner/admin mutations in service layer with centralized helper
- Decision: Implement mutation authorization checks in service methods before update/delete operations by validating authenticated user against deck author or admin role.
- Rationale: Requirement mandates rigorous ownership checks in service layer and matches existing authorization boundary patterns.
- Alternatives considered: Controller-only checks (rejected because it can be bypassed by future call paths); DB-trigger enforcement (rejected as harder to evolve and less expressive).

## Decision 3: Public/private read semantics inherited from deck visibility
- Decision: Allow anonymous and authenticated users to read cards for public decks; require owner/admin for private deck reads and all mutations.
- Rationale: Clarified spec decision and simplest policy for consistent deck-card access behavior.
- Alternatives considered: Require auth for all card reads (rejected as conflicting with clarified behavior); allow private reads to any authenticated user (rejected as privacy regression).

## Decision 4: Use Spring Data pagination defaults for all list/search endpoints
- Decision: Implement `Pageable`-driven pagination with explicit defaults (`page=0`, `size=20`) and upper bound (`size<=100`) on deck listing and deck-scoped card search/listing.
- Rationale: Constitution requires pagination on list endpoints, and Spring Data paging integrates natively with repositories.
- Alternatives considered: Custom offset/limit only (rejected due to higher implementation overhead and less consistency); unbounded lists (rejected by governance).

## Decision 5: Implement card search via repository query/specification with fixed semantics
- Decision: Implement deck-scoped search where `frontText` and `backText` are case-insensitive contains, and vocabulary search is case-insensitive exact.
- Rationale: Clarified matching rules remove ambiguity and support deterministic tests.
- Alternatives considered: Case-sensitive search (rejected due to lower usability); full-text external index (rejected as unnecessary complexity at current scale).

## Decision 6: Introduce S3 presigned PUT URL service with strict request validation
- Decision: Add AWS SDK v2 `s3` + `s3-presigner` integration and expose media-authorization endpoint that validates content type, extension coherence, and max size before issuing a 5-minute presigned PUT URL.
- Rationale: Satisfies security constraints that backend must not proxy multipart uploads and keeps media upload path scalable.
- Alternatives considered: Backend multipart upload proxy (rejected by constitution); pre-signed POST form flow (rejected because feature contract specifies presigned URL flow).

## Decision 7: Manage media object lifecycle by reference-awareness
- Decision: Delete physical media objects only when no remaining deck/card references exist; preserve object when still referenced.
- Rationale: Clarified requirement avoids data loss in shared-media scenarios.
- Alternatives considered: Immediate delete on first reference removal (rejected due to broken references); never delete (rejected due to storage growth risk).

## Decision 8: Apply per-user media authorization throttling at service boundary
- Decision: Reject media authorization requests over 30 per user per minute with deterministic error and telemetry emission.
- Rationale: Clarified anti-abuse rule protects public-facing authorization endpoint while preserving usability.
- Alternatives considered: No limit (rejected for abuse risk); much stricter limit (rejected due to UX friction for normal media edits).

## Decision 9: Preserve observability conventions with new event classes
- Decision: Emit structured events/metrics for deck/card CRUD outcomes, authorization denials, media validation failures, presigned URL issuance, and rate-limit rejections; map to existing New Relic + CloudWatch conventions.
- Rationale: Constitution requires day-one observability for changed runtime paths.
- Alternatives considered: Logging only failures (rejected because success/failure ratio is needed for operational diagnosis).

## Decision 10: Keep infrastructure changes additive and Terraform-governed
- Decision: Keep feature deploy topology unchanged; if new alarms/variables are required for media authorization metrics, add only through `infra/terraform` files.
- Rationale: Constitutional requirement mandates Terraform as sole IaC source.
- Alternatives considered: Manual cloud console alarm setup (rejected due to drift and governance violation).

## Validation Update

- All prior `NEEDS CLARIFICATION` items are resolved in `spec.md` clarifications.
- No unresolved technical-context unknowns remain after research.
- Research decisions align with constitution gates and existing repository structure.

## Implementation Confirmation (2026-03-16)

- US1 delivered with secure deck CRUD, pagination/filtering, and owner/admin mutation checks.
- US2 delivered with card create/update/delete plus deck-scoped search semantics:
	- `frontText` and `backText` case-insensitive contains.
	- `vocabulary` case-insensitive exact match.
- US3 delivered with media authorization endpoint, 5-minute presigned URL issuance, allowed type/size validation, and per-user throttling.
- Reference-aware media cleanup now triggers physical delete only when reference count reaches zero.
