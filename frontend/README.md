# KhaLeo Frontend Delivery Runbook

## Scope

This frontend delivers:

- Auth bootstrap and protected route guards.
- Deck/card CRUD, search, media upload validation, and study flow.
- Admin dashboard and moderation routes.

## Environment Contract

Copy `.env.example` to `.env` and set:

- `VITE_API_BASE_URL`
- `VITE_APP_ENV`
- `VITE_MEDIA_MAX_MB`

## Local Commands

```bash
npm install
npm run dev
npm run lint
npm run test
npm run test:coverage
npm run build
npm run preview
```

## E2E Smoke

```bash
npm run test:e2e:list
npm run test:e2e
```

Notes:

- Playwright config is in `playwright.config.ts`.
- Smoke specs are in `tests/e2e/smoke.spec.ts`.
- Feature 008 flow spec is in `tests/e2e/public-discovery-private-study.e2e.ts`.

## Discovery Workspace Session Flow

- `Decks` tab is guest-readable and lists only public decks.
- `Import to Study` requires sign-in; after auth it imports into learner private workspace.
- `Study` tab lists only private decks and links each deck to `Start session`.
- `Study session` route (`/study/session/:deckId`) runs two-sided flashcard flow with reveal and Again/Hard/Good/Easy ratings.

## Testing Layers

- Unit/state: `src/store/**/*.test.ts`
- Guards: `src/router/**/*.test.tsx`
- Feature journeys: `src/features/**/*.test.tsx`
- Contract drift checks: `src/services/contracts/**/*.test.ts`
- E2E smoke: `tests/e2e/`

## Deployment Expectations

- CI must pass lint, test coverage, and build.
- Production deploy uses OIDC role assumption and environment `production` approval.
- Frontend deploy pushes static assets to S3 and invalidates CloudFront.

## Troubleshooting

- If media validation behaves unexpectedly, check `VITE_MEDIA_MAX_MB`.
- If admin routes redirect away, log in with an email containing `admin` in local mock auth.
- If browser binaries are missing for E2E, run `npx playwright install chromium`.
