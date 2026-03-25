# Phase 0 Research: Public Deck Discovery, Private Workspace, and Re-import Merge

## Decision 1: Split product surfaces by intent (discovery vs personal workspace)
- Decision: Keep `Decks` as public discovery surface and `Study/Cards` as private learner workspace.
- Rationale: This matches user mental model and reduces accidental mutation risk on shared/public content.
- Alternatives considered: A single mixed list with visibility filters (rejected due to higher UX and authorization complexity).

## Decision 2: Allow guest read-only access to public discovery
- Decision: Public deck list/detail is guest-readable; import requires authentication.
- Rationale: Increases discoverability while keeping write-like operations tied to identity and ownership.
- Alternatives considered: Auth-required discovery (rejected for lower funnel conversion); guest import (rejected for ownership/audit ambiguity).

## Decision 3: Import creates/updates a private copy, never mutates source
- Decision: Import clones metadata/cards/media references into learner-owned private deck and excludes progress/history.
- Rationale: Preserves source integrity and prevents leaking another learner's progression context.
- Alternatives considered: Shared-progress import (rejected because progress semantics are user-specific and violates isolation expectations).

## Decision 4: Re-import uses merge semantics with explicit conflict handling
- Decision: Re-import merges source updates into the existing private copy; no-conflict paths preserve local edits; conflict paths require explicit user choice (local vs cloud).
- Rationale: Balances freshness from source updates with learner autonomy on private customizations.
- Alternatives considered: Always duplicate on re-import (rejected for workspace clutter); always overwrite with cloud (rejected for data loss risk); hard-block re-import (rejected for stale-content risk).

## Decision 5: Keep conflict decision at field/item granularity
- Decision: Conflict resolution is captured per conflicted unit (field/card/item), not as one global switch.
- Rationale: Learners can selectively keep personalized notes while accepting upstream corrections where needed.
- Alternatives considered: Global "all local" or "all cloud" decision (rejected because it is too coarse and increases bad-merge outcomes).

## Decision 6: Preserve constitutional FSRS v6 fidelity while adding first-step timing guarantees
- Decision: Keep existing Anki-style scheduler behavior for non-new cards and explicitly enforce new-card first-step timings (`Again=1m`, `Hard=6m`, `Good=10m`, `Easy=1d`).
- Rationale: Meets clarified UX expectation without introducing algorithm drift for mature cards.
- Alternatives considered: Replace scheduler wholesale with custom rules (rejected due to constitutional risk and regression surface).

## Decision 7: Enforce owner-based authorization boundaries on all private mutations
- Decision: Private deck/card CRUD and search operations execute only for learner-owned private decks (or admin paths when explicitly supported).
- Rationale: Prevents cross-user data modification and aligns with existing security model.
- Alternatives considered: Visibility-based mutable operations on public decks (rejected due to ownership violations and abuse risk).

## Decision 8: Treat import/re-import as observable, auditable runtime paths
- Decision: Emit structured success/failure signals for discovery access, import starts/completions, merge conflict prompts, decision outcomes, and authorization denials.
- Rationale: Merge workflows can fail partially; observability is required for diagnosis and support.
- Alternatives considered: Log only terminal failures (rejected because it hides user-facing conflict funnel and partial failure states).

## Decision 9: Keep persistence ownership in Aurora domain model
- Decision: Deck copy lineage, merge state, and conflict decisions are modeled in Aurora-owned transactional entities; DynamoDB remains event/activity oriented.
- Rationale: Import and merge are transactional domain behaviors tightly coupled to deck/card ownership semantics.
- Alternatives considered: Persist merge decisions in DynamoDB only (rejected due to weak transactional coupling for deterministic final state).

## Decision 10: Require deterministic pagination for discovery and workspace lists
- Decision: All list APIs in discovery and personal workspace define size bounds, continuation/page tokens, and stable ordering.
- Rationale: Satisfies constitutional API gate and prevents inconsistent UX under large datasets.
- Alternatives considered: Unbounded list retrieval (rejected for governance and scalability reasons).
