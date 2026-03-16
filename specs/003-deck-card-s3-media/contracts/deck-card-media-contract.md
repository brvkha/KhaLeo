# Deck/Card/Media Contract: Deck/Card Management and Media Uploads

## Purpose

Define external REST contracts for deck and card management plus S3 media upload authorization under `/api/v1`.

## General Contract Rules

- All responses are JSON except presigned URL field values.
- Authenticated identity is required for mutations.
- Public read behavior:
  - Public deck and public-deck card reads are allowed to unauthenticated users.
  - Private deck and private-deck card reads require owner/admin authorization.
- Mutation behavior:
  - Deck/card create, update, delete require owner/admin authorization.
- Pagination behavior:
  - List/search endpoints accept `page` (0-based) and `size`.
  - Default `size` is 20; max `size` is 100.

## Endpoint Contracts

### 1) POST `/api/v1/decks`

- Auth: required.
- Request body:
  - `name` (required)
  - `description` (optional)
  - `tags` (optional)
  - `isPublic` (required)
  - `coverImageUrl` (optional, must reference valid uploaded object if present)
- Success response:
  - `201 Created` with deck payload.
- Failure responses:
  - `400 Bad Request` validation errors.
  - `401 Unauthorized` missing/invalid auth.

### 2) GET `/api/v1/decks`

- Auth: optional.
- Query:
  - `isPublic` (optional)
  - `authorId` (optional)
  - `page` (optional)
  - `size` (optional)
- Success response:
  - `200 OK` paginated deck result (`content`, `page`, `size`, `totalElements`, `totalPages`).
- Failure responses:
  - `400 Bad Request` invalid pagination/filter parameters.

### 3) GET `/api/v1/decks/{id}`

- Auth: optional for public deck; required owner/admin for private deck.
- Success response:
  - `200 OK` deck detail payload.
- Failure responses:
  - `403 Forbidden` private deck access denied.
  - `404 Not Found` unknown deck.

### 4) PUT `/api/v1/decks/{id}`

- Auth: required owner/admin.
- Request body: mutable deck metadata fields.
- Success response:
  - `200 OK` updated deck payload.
- Failure responses:
  - `400 Bad Request` validation errors.
  - `403 Forbidden` authorization denied.
  - `404 Not Found` unknown deck.

### 5) DELETE `/api/v1/decks/{id}`

- Auth: required owner/admin.
- Success response:
  - `204 No Content`.
- Side effect contract:
  - Deletes dependent cards and learning-state records.
  - Dereferences media objects and deletes physical object only when no references remain.
- Failure responses:
  - `403 Forbidden` authorization denied.
  - `404 Not Found` unknown deck.

### 6) POST `/api/v1/decks/{deckId}/cards`

- Auth: required owner/admin of target deck.
- Request body:
  - `frontText` / `frontMediaUrl` (at least one required)
  - `backText` / `backMediaUrl` (at least one required)
- Success response:
  - `201 Created` card payload.
- Failure responses:
  - `400 Bad Request` invalid card content.
  - `403 Forbidden` authorization denied.
  - `404 Not Found` unknown deck.

### 7) PUT `/api/v1/cards/{id}`

- Auth: required owner/admin.
- Request body: mutable card content fields.
- Success response:
  - `200 OK` updated card payload.
- Failure responses:
  - `400 Bad Request` invalid card content.
  - `403 Forbidden` authorization denied.
  - `404 Not Found` unknown card.

### 8) DELETE `/api/v1/cards/{id}`

- Auth: required owner/admin.
- Success response:
  - `204 No Content`.
- Side effect contract:
  - Removes card and related learning states.
  - Dereferences media objects and deletes physical object only when no references remain.
- Failure responses:
  - `403 Forbidden` authorization denied.
  - `404 Not Found` unknown card.

### 9) GET `/api/v1/decks/{deckId}/cards/search`

- Auth: optional for public deck; required owner/admin for private deck.
- Query:
  - `frontText` (optional, case-insensitive contains)
  - `backText` (optional, case-insensitive contains)
  - `vocabulary` (optional, case-insensitive exact)
  - `page` (optional)
  - `size` (optional)
- Success response:
  - `200 OK` paginated card result.
- Failure responses:
  - `400 Bad Request` invalid query or pagination parameters.
  - `403 Forbidden` private deck access denied.
  - `404 Not Found` unknown deck.

### 10) GET `/api/v1/media/presigned-url`

- Auth: required.
- Query:
  - `fileName` (required)
  - `contentType` (required)
  - `sizeBytes` (required)
- Validation contract:
  - Allowed content types: `image/jpeg`, `image/png`, `audio/mpeg`, `audio/webm`.
  - `sizeBytes <= 5242880`.
  - Extension and content type must be coherent.
  - Rate limit: max 30 requests per user per minute.
- Success response:
  - `200 OK` with:
    - `objectKey`
    - `uploadUrl`
    - `expiresInSeconds` (= 300)
- Failure responses:
  - `400 Bad Request` invalid metadata/type/size.
  - `401 Unauthorized` missing/invalid auth.
  - `429 Too Many Requests` rate-limit exceeded.

## Error Envelope Contract

Error responses follow existing JSON envelope:

- `timestamp`
- `status`
- `error` (stable machine-readable code)
- `message` (user-safe text)
- `path`

### Error Codes (feature-specific additions)

- `AUTHORIZATION_DENIED`
- `DECK_NOT_FOUND`
- `CARD_NOT_FOUND`
- `INVALID_PAGINATION`
- `INVALID_SEARCH_CRITERIA`
- `MEDIA_TYPE_NOT_ALLOWED`
- `MEDIA_SIZE_EXCEEDED`
- `MEDIA_EXTENSION_MISMATCH`
- `MEDIA_AUTH_RATE_LIMIT_EXCEEDED`
- `MEDIA_REFERENCE_CONFLICT`

## Observability Contract

- Emit structured events for:
  - deck/card create/update/delete success and failure
  - access denial for private-read or mutation authorization failures
  - media authorization request accepted/rejected outcomes
  - rate-limit rejections
  - media object deletion deferred due to remaining references
- Do not log secrets or raw auth tokens.
- Metric names and alarm dimensions must remain compatible with existing `KhaLeo/Backend` conventions.

## Compatibility Expectations

- New endpoints must not break existing auth endpoint contracts.
- Pagination fields and error envelope shape must remain stable across revisions.
- Contract updates require synchronized updates to `spec.md`, `plan.md`, and tests.
