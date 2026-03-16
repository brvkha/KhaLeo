# Study Engine Contract: SM-2 Spaced Repetition and Study Activity Logging

## Purpose

Define the public REST contract for due-card retrieval and card-rating operations under `/api/v1/study`.

## General Contract Rules

- All endpoints return JSON.
- Authenticated user context is required for all study endpoints.
- User may only access cards in decks they are authorized to study.
- Rating persistence in Aurora is synchronous for request success; activity logging to DynamoDB is asynchronous and non-blocking for response.
- Pagination is mandatory for list-producing next-cards endpoint.

## Endpoint Contracts

### 1) GET `/api/v1/study/decks/{deckId}/next-cards`

- Auth: required.
- Purpose: Retrieve due cards prioritized by urgency and daily-limit rules.
- Query parameters:
  - `size` (optional, integer, default 20, max 100)
  - `continuationToken` (optional, opaque token for next page)
- Success response:
  - `200 OK`
  - Body:
    - `items` (array of due/new-eligible card summaries)
    - `nextContinuationToken` (string or null)
    - `hasMore` (boolean)
- Selection contract:
  - Items are ordered by tier: due learning, then due review, then new eligible cards.
  - New cards are included only when account daily unique-card limit has remaining quota.
- Failure responses:
  - `400 Bad Request` invalid `size` or malformed continuation token.
  - `401 Unauthorized` missing/invalid auth.
  - `403 Forbidden` deck inaccessible to caller.
  - `404 Not Found` unknown deck.

### 2) POST `/api/v1/study/cards/{cardId}/rate`

- Auth: required.
- Purpose: Apply learner rating and update scheduling state.
- Request body:
  - `rating` (required enum: `AGAIN`, `HARD`, `GOOD`, `EASY`)
  - `timeSpentMs` (required integer >= 0)
- Success response:
  - `200 OK`
  - Body includes:
    - `cardId`
    - `state`
    - `nextReviewAt`
    - `newInterval`
    - `newEaseFactor`
- Behavior contract:
  - Applies SM-2 logic and state transitions.
  - Enforces minimum ease factor of 1.3.
  - Triggers asynchronous activity log write with final schedule values.
- Failure responses:
  - `400 Bad Request` invalid rating/time input.
  - `401 Unauthorized` missing/invalid auth.
  - `403 Forbidden` caller not authorized for target card.
  - `404 Not Found` unknown card.

## Error Envelope Contract

Error responses use standardized JSON shape:

- `timestamp`: RFC3339 timestamp
- `status`: HTTP status code
- `error`: machine-readable code
- `message`: user-safe description
- `path`: request path

### Error Codes

- `INVALID_REQUEST`
- `INVALID_PAGINATION`
- `DECK_NOT_FOUND`
- `CARD_NOT_FOUND`
- `AUTHORIZATION_DENIED`
- `RATING_INVALID`
- `DAILY_LIMIT_REACHED_FOR_NEW_CARDS`

## Observability Contract

- Emit structured events for:
  - next-cards retrieval success/failure/denial
  - rating acceptance and validation rejection
  - async activity-log write success/failure
- Include correlation/request identifiers in all logs.
- Never log secrets, JWT values, or sensitive payload data.

## Pagination Clause

- `GET /next-cards` is paginated via `size` and `continuationToken`.
- Clients continue retrieval while `hasMore=true` and `nextContinuationToken` is present.
- Server must enforce max page size and reject invalid pagination inputs deterministically.
