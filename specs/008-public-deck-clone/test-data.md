# Test Data Fixture: Security + Public Deck Discovery

## Scope

This fixture defines reusable QA/E2E data for the policy:

- Only `/decks` is guest-accessible.
- All non-`/decks` routes require login.
- Protected APIs require JWT.
- Public deck actions that need auth must redirect to login with `returnTo`.

## Sample Login Accounts

Use the same password for all seeded accounts:

- Password: `khaleo`

| Role | Email | Status | Purpose |
|---|---|---|---|
| ADMIN | `admin@khaleo.app` | verified, active | Admin dashboard and moderation tests |
| USER | `khaleo@khaleo.app` | verified, active | Quick smoke login account |
| USER | `learner+01@khaleo.app` | verified, active | Main learner smoke flow |
| USER | `learner+02@khaleo.app` | verified, active | Cross-user ownership tests |
| USER | `learner+03@khaleo.app` | verified, active | Import/re-import tests |
| USER | `learner+04@khaleo.app` | verified, active | Search/pagination tests |
| USER | `learner+05@khaleo.app` | verified, active | Study-session load tests |
| USER | `learner+blocked@khaleo.app` | verified, banned | Blocked-account enforcement tests |
| USER | `learner+unverified@khaleo.app` | unverified, active | Verification-required tests |

## Dataset Size Baseline

- Users: 8
- Decks: 12 total
- Public decks: 6
- Private decks: 6
- Cards per deck: 10
- Total cards: 120

## Deck Ownership Matrix

| Deck Code | Visibility | Owner | Card Count |
|---|---|---|---|
| PUB-EN-01 | public | learner+01 | 10 |
| PUB-EN-02 | public | learner+02 | 10 |
| PUB-EN-03 | public | learner+03 | 10 |
| PUB-MATH-01 | public | learner+04 | 10 |
| PUB-SCI-01 | public | learner+05 | 10 |
| PUB-HIST-01 | public | learner+01 | 10 |
| PRI-L1-01 | private | learner+01 | 10 |
| PRI-L2-01 | private | learner+02 | 10 |
| PRI-L3-01 | private | learner+03 | 10 |
| PRI-L4-01 | private | learner+04 | 10 |
| PRI-L5-01 | private | learner+05 | 10 |
| PRI-ADMIN-01 | private | admin@khaleo.app | 10 |

## Card Content Pattern

Per deck, generate 10 cards with deterministic text:

- frontText: `<DECK_CODE>-Q-<01..10>`
- backText: `<DECK_CODE>-A-<01..10>`
- vocabulary: `<deck-topic>-term-<01..10>`

Example for `PUB-EN-01`:

- `PUB-EN-01-Q-01` -> `PUB-EN-01-A-01`
- `PUB-EN-01-Q-02` -> `PUB-EN-01-A-02`
- ...
- `PUB-EN-01-Q-10` -> `PUB-EN-01-A-10`

## Required Verification Flows

1. Guest can open `/decks` and see only 6 public decks.
2. Guest opening `/study`, `/cards`, `/profile`, or `/admin` is redirected to `/login?returnTo=...`.
3. Guest clicking auth-required action from `/decks` is redirected to login and returned to intended target after sign-in.
4. `learner+01@khaleo.app` can mutate only `PRI-L1-01` and cannot mutate `PRI-L2-01`.
5. `learner+unverified@khaleo.app` is denied import and study actions.
6. `learner+blocked@khaleo.app` is denied protected actions immediately.
