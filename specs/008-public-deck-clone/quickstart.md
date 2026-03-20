# Quickstart: Public Deck Discovery, Private Workspace, and Re-import Merge

## 1. Prerequisites

- Java 17 and Maven installed for backend changes.
- Node.js 20+ for frontend changes.
- Existing auth flow available for guest/user role path checks.
- Local database/test environment prepared per project standard setup.
- Seed fixture from `test-data.md` is loaded (accounts, 12 decks, 120 cards).

## 2. Implement Discovery and Workspace Segmentation

1. Backend:
   - Add/adjust discovery endpoint(s) to return only `PUBLIC` decks.
   - Add/adjust workspace endpoint(s) to return only learner-owned `PRIVATE` decks.
   - Ensure all list endpoints use bounded pagination.
2. Frontend:
   - `Decks` tab: public discovery list (guest-readable).
   - `Study/Cards` tabs: learner private workspace only.

## 3. Implement Import and Re-import Domain Flow

1. Add import endpoint/service:
   - Requires authenticated user.
   - Creates private copy with deck metadata, cards, and media references.
   - Explicitly excludes learning progress/history.
2. Add re-import endpoint/service:
   - Locates existing import lineage.
   - Merges source updates into private copy.
   - Preserves local edits when no conflict exists.

## 4. Implement Merge Conflict Decision Path

1. Detect conflicts at field/item granularity.
2. Persist conflict snapshots (`localValue`, `cloudValue`) until resolved.
3. Provide resolution API/UI to choose `LOCAL` or `CLOUD` for each conflict unit.
4. Finalize merge only after all conflict units are resolved.

## 5. Preserve Study Session and Scheduling Behavior

1. Keep two-sided flashcard session flow in UI.
2. Enforce new-card first-step timing:
   - `Again`: 1 minute
   - `Hard`: 6 minutes
   - `Good`: 10 minutes
   - `Easy`: 1 day
3. Preserve existing Anki-style scheduler behavior for non-new cards and account-level daily limits.

## 6. Observability and Security Validation

1. Add/verify structured logs and metrics for:
   - public discovery access
   - import/re-import start and finish
   - merge conflict creation/resolution
   - authorization denials
   - study rating outcomes
2. Validate auth boundaries:
   - guest can view public decks
   - guest cannot import
   - guest can access only `/decks`; all other routes redirect to login with `returnTo`
   - private workspace mutations remain owner-only.

## 7. Verify with Targeted Tests

### Backend

```bash
cd backend
mvn test
mvn -Dtest="*Deck*Import*" test
mvn -Dtest="*Merge*Conflict*" test
mvn -Dtest="*Study*" test
```

### Frontend

```bash
cd frontend
npm test
npm run test:e2e
```

## 8. End-to-End Manual Verification Script

1. Open app as guest:
   - Confirm `Decks` shows only public decks.
   - Confirm import action requires login.
2. Login as learner:
   - Import one public deck.
   - Confirm private copy appears in `Study/Cards` only.
3. Modify private copy locally.
4. Update source public deck as source owner (or via fixture).
5. Re-import as learner:
   - Confirm merge applies.
   - If conflict appears, select local/cloud per conflict and complete merge.
6. Start study session:
   - Confirm two-sided cards.
   - Confirm first-step timing for new cards matches 1m/6m/10m/1d mapping.
7. Route/auth policy checks:
   - Open `/study` directly as guest and verify redirect to login preserves target.
   - Sign in and verify app returns to preserved target.

## 9. Evidence to Capture

- Contract test output for discovery/import/re-import/conflict endpoints.
- Integration tests for merge outcomes (no conflict vs conflict required).
- E2E test output for guest vs authenticated behavior in UI tabs.
- Scheduler test evidence for new-card timing and non-new behavior preservation.
- Sample logs/metrics for import/merge/conflict telemetry.

### Verification Log (2026-03-20)

- Frontend targeted tests:
   - `npm.cmd run test -- src/test/study-session/twoSidedFlashcardFlow.test.tsx src/test/study-workspace/privateWorkspaceVisibility.test.tsx`
   - Result: pass (2 files, 2 tests).
- Backend targeted scheduler/session tests:
   - `StudyTimingMappingUnitTest`
   - `StudySessionFlowIntegrationTest`
   - `StudySchedulerLegacyBehaviorIntegrationTest`
   - Result: pass in workspace test runner for selected files.

## 10. Modular Boundary Guardrail

- Keep `importmerge`, `study`, and `deck` modules separated by service boundaries:
   - `importmerge` may query deck/card repositories and write import lineage/conflict entities.
   - `study` may consume deck/card access through `StudyAccessService` and must not directly implement import merge policies.
   - `deck` controllers/services own discovery and workspace listing semantics.
- Shared cross-cutting behavior (telemetry, verification guard, pagination) belongs in dedicated shared services/config and is referenced by modules, not re-implemented per module.
