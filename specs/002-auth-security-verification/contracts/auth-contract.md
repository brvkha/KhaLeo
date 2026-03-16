# Authentication Contract: Authentication, Security, and Identity Verification

## Purpose

Define the public REST contract for authentication and identity-verification
flows exposed under `/api/v1/auth`.

## General Contract Rules

- All endpoints in this document are public and do not require prior bearer auth.
- Response bodies are JSON.
- Sensitive outcomes must avoid account enumeration details (especially forgot-password and login failures).
- Access token lifetime: 15 minutes.
- Refresh token lifetime: 7 days.
- Unverified users cannot obtain tokens.

## Endpoint Contracts

### 1) POST `/api/v1/auth/register`

- Request body:
  - `email` (required, valid email format)
  - `password` (required, complies with password policy)
- Success response:
  - `201 Created`
  - Body includes registration acknowledgement and verification-required status.
- Failure responses:
  - `400 Bad Request` for validation failures.
  - `409 Conflict` when email already exists.

### 2) GET `/api/v1/auth/verify?token={token}`

- Query:
  - `token` (required)
- Success response:
  - `200 OK`
  - Body confirms account verification success.
- Failure responses:
  - `400 Bad Request` for malformed token.
  - `410 Gone` for expired token.
  - `404 Not Found` for unknown token.

### 3) POST `/api/v1/auth/login`

- Request body:
  - `email` (required)
  - `password` (required)
- Success response:
  - `200 OK`
  - Body includes `accessToken`, `refreshToken`, and `expiresIn`.
- Failure responses:
  - `401 Unauthorized` for invalid credentials.
  - `403 Forbidden` for unverified accounts.
  - `423 Locked` for locked account with lockout expiration metadata.

### 4) POST `/api/v1/auth/refresh`

- Request body:
  - `refreshToken` (required)
- Success response:
  - `200 OK`
  - Body includes new `accessToken` and `expiresIn`.
- Failure responses:
  - `401 Unauthorized` for invalid, revoked, or expired refresh token.

### 5) POST `/api/v1/auth/forgot-password`

- Request body:
  - `email` (required)
- Success response:
  - `202 Accepted`
  - Body is generic and does not reveal account existence.
- Failure responses:
  - `400 Bad Request` for malformed email only.

### 6) POST `/api/v1/auth/reset-password`

- Request body:
  - `token` (required)
  - `newPassword` (required, password policy compliant)
- Success response:
  - `200 OK`
  - Body confirms password reset completion.
- Failure responses:
  - `400 Bad Request` for malformed token/password.
  - `410 Gone` for expired token.
  - `404 Not Found` for unknown token.

### 7) POST `/api/v1/auth/logout`

- Request body:
  - `refreshToken` (required)
- Success response:
  - `204 No Content` (idempotent behavior accepted).
- Failure responses:
  - `400 Bad Request` for malformed input.

## Error Envelope Contract

Errors for this feature use a standardized JSON shape:

- `timestamp`: RFC3339 timestamp
- `status`: HTTP status code
- `error`: stable machine-readable code (for example `ACCOUNT_LOCKED`)
- `message`: user-safe summary
- `path`: request path

### Error Codes

- `INVALID_REQUEST`
- `DUPLICATE_EMAIL`
- `INVALID_VERIFICATION_TOKEN`
- `EXPIRED_VERIFICATION_TOKEN`
- `UNVERIFIED_EMAIL`
- `INVALID_CREDENTIALS`
- `ACCOUNT_LOCKED`
- `INVALID_REFRESH_TOKEN`
- `EXPIRED_REFRESH_TOKEN`
- `INVALID_RESET_TOKEN`
- `EXPIRED_RESET_TOKEN`

## Security And Logging Contract

- Log structured security events for each auth endpoint invocation outcome.
- Never log plaintext passwords, raw JWT secrets, or reset/verification token values.
- Include correlation IDs to connect authentication events across services and telemetry.

## Pagination Clause

- This feature introduces no list-producing endpoints. Pagination requirements are not applicable.
