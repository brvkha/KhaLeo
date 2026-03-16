# Phase 0 Research: Authentication, Security, and Identity Verification

## Decision 1: Keep stateless JWT model with persisted refresh-token revocation
- Decision: Use short-lived access tokens (15m) plus persisted refresh tokens (7d) with explicit server-side revocation.
- Rationale: Satisfies constitutional token lifetime requirements while enabling logout and compromised-session invalidation.
- Alternatives considered: Access-token-only model (rejected due to poor UX and forced frequent re-authentication); fully stateful server sessions (rejected due to constitution requirement for stateless JWT authentication).

## Decision 2: Block authentication until email ownership is verified
- Decision: Require successful email-verification token redemption before issuing any login tokens.
- Rationale: Prevents unowned or typo email accounts from becoming active identities and aligns with security-by-default governance.
- Alternatives considered: Allow login before verification with feature restrictions (rejected due to inconsistent security boundary); no verification gate (rejected as constitutional violation).

## Decision 3: Use Aurora as authoritative store for auth and token lifecycle
- Decision: Store refresh-token, verification-token, and reset-token state in Aurora alongside user lockout counters.
- Rationale: Keeps identity state transactional and revocation checks deterministic in the primary relational source of truth.
- Alternatives considered: DynamoDB token storage (rejected due to cross-store consistency complexity for auth-critical reads); in-memory token tracking (rejected because it breaks horizontal scalability and restart safety).

## Decision 4: Implement deterministic lockout policy in user identity record
- Decision: Track `failedLoginAttempts` and `accountLockedUntil` on user records; lock account for exactly 24h on the fifth consecutive failure and reset counter on successful login.
- Rationale: Enforces anti-brute-force behavior with clear deterministic rules and minimal query overhead.
- Alternatives considered: IP-only rate limiting (rejected due to weak account-specific protection); progressive backoff only (rejected because requirement specifies fixed lockout threshold and duration).

## Decision 5: Use one-time expiring tokens for verification and password reset
- Decision: Verification tokens expire after 24h; reset tokens expire after 1h; successful consumption invalidates token immediately.
- Rationale: Limits replay risk and constrains attack window while preserving usability expectations.
- Alternatives considered: Non-expiring links (rejected due to security risk); password reset by OTP only (rejected as additional complexity not requested in scope).

## Decision 6: Use AWS SES SDK v2 for transactional auth emails
- Decision: Send registration-verification and password-reset emails through AWS SES via SDK v2 integration.
- Rationale: Matches platform stack, avoids SMTP transport complexity, and aligns with explicit technical constraints.
- Alternatives considered: SMTP via JavaMail (rejected by spec constraints); third-party email API outside AWS (rejected due to additional vendor and governance overhead).

## Decision 7: Standardize auth error response contract
- Decision: Return JSON error envelopes for 401/403 and auth-domain failures (invalid credentials, unverified account, locked account, invalid token, expired token) without leaking sensitive existence details.
- Rationale: Supports secure client handling, avoids HTML error pages, and improves observability consistency.
- Alternatives considered: Framework default responses (rejected due to inconsistent shape and potential information leakage); highly verbose error detail (rejected due to account-enumeration risk).

## Decision 8: Define layered testing strategy for auth correctness and abuse resistance
- Decision: Combine unit tests (token, lockout, validation), integration tests (auth endpoint and persistence paths), and contract tests (response shape and migration constraints).
- Rationale: Verifies critical security behavior at appropriate layers while aligning with mandated quality gates.
- Alternatives considered: Unit-only strategy (rejected because endpoint/security-chain behavior would remain unverified); end-to-end-only strategy (rejected due to low diagnostic precision and slower feedback).

## Decision 9: Security observability events for all auth lifecycle transitions
- Decision: Emit structured events for registration, verification send/confirm, login success/failure, lockout trigger, refresh success/failure, reset request/confirm, and logout revocation.
- Rationale: Required by observability governance and necessary for incident detection and forensic analysis.
- Alternatives considered: Error-only logging (rejected for insufficient auditability); debug-level-only telemetry (rejected because production alerting requires durable structured events).

## Decision 10: Infrastructure impact remains additive and Terraform-governed
- Decision: Keep deployment topology unchanged and route any SES/monitoring configuration updates through Terraform modules and variables.
- Rationale: Preserves constitutional infrastructure constraints and avoids unmanaged cloud drift.
- Alternatives considered: Manual console configuration (rejected by IaC governance); introducing new runtime components for auth (rejected as unnecessary complexity).

## Validation Update

- 2026-03-16 implementation validation confirmed Decision 1 through Decision 10 in executable code and tests.
- Contract, integration, and unit tests now cover verification gating, token lifecycle, lockout policy, password reset, and refresh-token revocation.
