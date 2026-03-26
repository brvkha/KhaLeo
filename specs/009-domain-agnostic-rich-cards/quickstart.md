# Quickstart: Domain-Agnostic Rich Card Content and Study UI Layout

## 1. Prerequisites

- Java 17 and Maven for backend.
- Node.js 20+ for frontend.
- Existing local stack for backend/frontend/database.
- Flyway migrations enabled in backend runtime.

## 2. Implement Backend Contract and Validation

1. Extend card DTO/entity mapping for:
   - `term`, `answer`, `imageUrl`, `partOfSpeech`, `phonetic`, `examples`.
2. Enforce validation:
   - examples max 20
   - each example trimmed non-empty, <= 300 chars
   - image URL absolute HTTPS and host allowlisted
   - payload max 64KB
3. Enforce full-object replace update semantics.
4. Enforce optimistic concurrency and return `409 Conflict` on stale version token.

## 3. Implement Flyway Migration

1. Add additive migration for rich-card fields.
2. Backfill old records with defaults:
   - `imageUrl = null`
   - `examples = []`
3. Ensure migration is idempotent and safe to rerun.

## 4. Implement Frontend Create/Edit Updates

1. Replace old front/back-only editor with rich-card form fields.
2. Build examples editor using row-level add/remove controls.
3. Block add action when count is 20.
4. Show field-level validation messages from client and server.
5. In preview, hide metadata rows when value is null.

## 5. Implement Study View Layout Changes

1. Render card content order on back side:
   - term -> image -> answer -> phonetic + partOfSpeech -> examples.
2. Render image on both faces when present.
3. Use placeholder + error icon when image fails to load.
4. Keep only examples region scrollable; keep core content fixed/visible.
5. Reset examples scroll position on card flip.
6. Show rating controls only on back face.
7. On mobile, fix rating controls to bottom-center and apply safe-area inset.
8. On desktop/tablet, keep natural (non-fixed) rating placement.

## 6. Verify Scheduling Invariance

1. Execute regression tests against existing FSRS v6 rating paths.
2. Confirm no changes in rating-to-schedule outputs for same inputs.

## 7. Test Commands

### Backend

```bash
cd backend
mvn test
mvn -Dtest="*Card*Validation*" test
mvn -Dtest="*Flyway*" test
mvn -Dtest="*Study*FSRS*" test
```

### Frontend

```bash
cd frontend
npm.cmd test
npm.cmd run test:e2e
```

## 8. Manual Verification Script

1. Open card create page and create a rich card with valid data.
2. Try adding 21st example and confirm UI blocks action.
3. Save invalid URL/non-allowlisted URL and confirm rejection.
4. Edit same card from two sessions and verify stale update gets `409`.
5. Start study on mobile viewport and verify fixed bottom rating controls appear only on back face.
6. Confirm only examples area scrolls and resets after flip.
7. Simulate image-load failure and verify placeholder/icon fallback.
8. Re-run an existing FSRS regression scenario and confirm unchanged outputs.

## 9. Evidence to Capture

- Backend test outputs for migration, validation, and version-conflict checks.
- Frontend tests for create/edit validation and study layout behavior.
- E2E evidence for mobile bottom rating controls and image fallback.
- Regression evidence proving unchanged FSRS v6 scheduling outputs.

## 10. Implementation Notes

- Backend create/update now logs structured telemetry on rich-card create/update failures.
- Study session card payload includes rich fields (`term`, `answer`, `imageUrl`, `partOfSpeech`, `phonetic`, `examples`) while preserving legacy `frontText`/`backText` for compatibility.
- Study rendering uses ordered back-face layout and an examples-only scroll container that resets on flip.
- Mobile rating controls use a fixed-bottom bar with safe-area inset handling; desktop/tablet keeps natural flow.

## 11. Verification Checklist

- [X] Run backend targeted suite: rich-card contract + migration + concurrency + FSRS regression.
- [X] Run frontend targeted unit suite for rich editor + study session layout/scroll/fallback + rating visibility regression.
- [X] Run Playwright mobile scenario for fixed-bottom rating bar behavior.
- [X] Confirm telemetry events emitted for `rich_card_create_failed`, `rich_card_update_failed`, and validation outcomes.
- [X] Confirm no FSRS v6 schedule behavior changes for existing rating paths.
