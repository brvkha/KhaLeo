# Tasks: Authentication, Security, and Identity Verification

**Input**: Design documents from `/specs/002-auth-security-verification/`
**Prerequisites**: plan.md (required), spec.md (required), research.md, data-model.md, contracts/auth-contract.md, quickstart.md

**Tests**: Include unit, integration, and contract tests to satisfy constitutional quality gates for authentication, security, persistence, and observability behavior.

**Organization**: Tasks are grouped by user story so each story can be implemented and validated independently.

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Prepare auth-focused project scaffolding and environment configuration references.

- [X] T001 Add authentication-related dependency declarations in backend/pom.xml
- [X] T002 Create auth package scaffolding via package-info classes in backend/src/main/java/com/khaleo/flashcard/service/auth/package-info.java and backend/src/main/java/com/khaleo/flashcard/controller/auth/package-info.java
- [X] T003 [P] Add auth configuration properties section in backend/src/main/resources/application.yml
- [X] T004 [P] Add auth test package scaffolding via package-info classes in backend/src/test/java/com/khaleo/flashcard/unit/auth/package-info.java, backend/src/test/java/com/khaleo/flashcard/integration/auth/package-info.java, and backend/src/test/java/com/khaleo/flashcard/contract/auth/package-info.java

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Build core security, persistence, and observability foundations required before any user-story implementation.

**CRITICAL**: No user story work starts before this phase is complete.

- [X] T005 Create Flyway migration for auth schema extensions and token tables in backend/src/main/resources/db/migration/V2__auth_security_schema.sql
- [X] T006 [P] Extend user entity with lockout counters and verification defaults in backend/src/main/java/com/khaleo/flashcard/entity/User.java
- [X] T007 [P] Create RefreshToken entity in backend/src/main/java/com/khaleo/flashcard/entity/RefreshToken.java
- [X] T008 [P] Create EmailVerificationToken entity in backend/src/main/java/com/khaleo/flashcard/entity/EmailVerificationToken.java
- [X] T009 [P] Create PasswordResetToken entity in backend/src/main/java/com/khaleo/flashcard/entity/PasswordResetToken.java
- [X] T010 [P] Create repositories for auth token entities in backend/src/main/java/com/khaleo/flashcard/repository/RefreshTokenRepository.java, backend/src/main/java/com/khaleo/flashcard/repository/EmailVerificationTokenRepository.java, and backend/src/main/java/com/khaleo/flashcard/repository/PasswordResetTokenRepository.java
- [X] T011 Configure stateless Spring Security filter chain and auth endpoint whitelist in backend/src/main/java/com/khaleo/flashcard/config/security/SecurityConfig.java
- [X] T012 [P] Implement JWT token provider and claim validation utility in backend/src/main/java/com/khaleo/flashcard/service/auth/JwtTokenService.java
- [X] T013 [P] Implement JWT authentication filter for SecurityContext population in backend/src/main/java/com/khaleo/flashcard/config/security/JwtAuthenticationFilter.java
- [X] T014 [P] Implement standardized JSON auth exception response handling in backend/src/main/java/com/khaleo/flashcard/config/security/AuthExceptionHandler.java
- [X] T015 [P] Implement SES email dispatch adapter for auth templates in backend/src/main/java/com/khaleo/flashcard/service/auth/SesEmailService.java
- [X] T016 [P] Add structured auth security-event logging hooks in backend/src/main/java/com/khaleo/flashcard/service/auth/AuthAuditLogger.java

**Checkpoint**: Foundation ready for user-story implementation.

---

## Phase 3: User Story 1 - Register and Verify Identity (Priority: P1) 🎯 MVP

**Goal**: Enable account registration with mandatory email verification before login token issuance.

**Independent Test**: Register a new account, confirm login is blocked while unverified, verify account via token link, and confirm verification token cannot be reused.

### Tests for User Story 1

- [X] T017 [P] [US1] Add contract test for POST /api/v1/auth/register and GET /api/v1/auth/verify in backend/src/test/java/com/khaleo/flashcard/contract/auth/AuthRegistrationVerificationContractTest.java
- [X] T018 [P] [US1] Add integration test for register-to-verify flow in backend/src/test/java/com/khaleo/flashcard/integration/auth/AuthRegistrationVerificationIT.java
- [X] T019 [P] [US1] Add integration test ensuring unverified login denial in backend/src/test/java/com/khaleo/flashcard/integration/auth/UnverifiedLoginBlockedIT.java
- [X] T020 [P] [US1] Add unit tests for verification token lifecycle rules in backend/src/test/java/com/khaleo/flashcard/unit/auth/EmailVerificationTokenServiceTest.java

### Implementation for User Story 1

- [X] T021 [P] [US1] Implement registration request/response DTOs in backend/src/main/java/com/khaleo/flashcard/controller/auth/dto/RegisterRequest.java and backend/src/main/java/com/khaleo/flashcard/controller/auth/dto/RegisterResponse.java
- [X] T022 [P] [US1] Implement verification endpoint request/response DTOs in backend/src/main/java/com/khaleo/flashcard/controller/auth/dto/VerifyEmailRequest.java and backend/src/main/java/com/khaleo/flashcard/controller/auth/dto/VerifyEmailResponse.java
- [X] T023 [US1] Implement registration and verification business logic in backend/src/main/java/com/khaleo/flashcard/service/auth/RegistrationService.java
- [X] T024 [US1] Implement register and verify endpoints in backend/src/main/java/com/khaleo/flashcard/controller/auth/AuthRegistrationController.java
- [X] T025 [US1] Enforce verification prerequisite in authentication entry flow in backend/src/main/java/com/khaleo/flashcard/service/auth/AuthenticationService.java
- [X] T026 [US1] Add auth observability events for registration, verification-send, and verification-complete outcomes in backend/src/main/java/com/khaleo/flashcard/service/auth/AuthAuditLogger.java

**Checkpoint**: User Story 1 is independently functional and testable.

---

## Phase 4: User Story 2 - Authenticate and Stay Signed In Securely (Priority: P2)

**Goal**: Issue access/refresh tokens to verified users, refresh access with valid refresh token, and revoke refresh token on logout.

**Independent Test**: Login with verified account to receive tokens, refresh access token with refresh token, logout, and confirm revoked refresh token is rejected.

### Tests for User Story 2

- [X] T027 [P] [US2] Add contract test for POST /api/v1/auth/login, /refresh, and /logout in backend/src/test/java/com/khaleo/flashcard/contract/auth/AuthTokenLifecycleContractTest.java
- [X] T028 [P] [US2] Add integration test for login-refresh-logout lifecycle in backend/src/test/java/com/khaleo/flashcard/integration/auth/AuthTokenLifecycleIT.java
- [X] T029 [P] [US2] Add integration test for revoked/expired refresh rejection in backend/src/test/java/com/khaleo/flashcard/integration/auth/RefreshTokenRejectionIT.java
- [X] T030 [P] [US2] Add unit tests for JWT claims and expiry handling in backend/src/test/java/com/khaleo/flashcard/unit/auth/JwtTokenServiceTest.java

### Implementation for User Story 2

- [X] T031 [P] [US2] Implement login/refresh/logout DTOs in backend/src/main/java/com/khaleo/flashcard/controller/auth/dto/LoginRequest.java, backend/src/main/java/com/khaleo/flashcard/controller/auth/dto/LoginResponse.java, backend/src/main/java/com/khaleo/flashcard/controller/auth/dto/RefreshTokenRequest.java, and backend/src/main/java/com/khaleo/flashcard/controller/auth/dto/LogoutRequest.java
- [X] T032 [US2] Implement login and refresh-token issuance flow in backend/src/main/java/com/khaleo/flashcard/service/auth/AuthenticationService.java
- [X] T033 [US2] Implement refresh token validation and new access issuance flow in backend/src/main/java/com/khaleo/flashcard/service/auth/TokenRefreshService.java
- [X] T034 [US2] Implement refresh token revocation on logout in backend/src/main/java/com/khaleo/flashcard/service/auth/LogoutService.java
- [X] T035 [US2] Implement login, refresh, and logout endpoints in backend/src/main/java/com/khaleo/flashcard/controller/auth/AuthSessionController.java
- [X] T036 [US2] Add auth observability events for login success/failure, refresh success/failure, and logout revocation in backend/src/main/java/com/khaleo/flashcard/service/auth/AuthAuditLogger.java

**Checkpoint**: User Story 2 is independently functional and testable.

---

## Phase 5: User Story 3 - Recover Account and Resist Brute-Force Attempts (Priority: P3)

**Goal**: Enforce lockout after repeated failed logins and provide secure password reset flow that invalidates existing refresh tokens.

**Independent Test**: Trigger five failed login attempts to lock account, verify lockout response, request password reset, complete reset with valid token, and confirm prior refresh tokens are invalidated.

### Tests for User Story 3

- [X] T037 [P] [US3] Add contract test for POST /api/v1/auth/forgot-password and /reset-password in backend/src/test/java/com/khaleo/flashcard/contract/auth/AuthPasswordResetContractTest.java
- [X] T038 [P] [US3] Add integration test for failed-login lockout threshold and duration in backend/src/test/java/com/khaleo/flashcard/integration/auth/LoginLockoutPolicyIT.java
- [X] T039 [P] [US3] Add integration test for forgot-password and reset flow with token expiry in backend/src/test/java/com/khaleo/flashcard/integration/auth/PasswordResetFlowIT.java
- [X] T040 [P] [US3] Add integration test for refresh-token invalidation after password reset in backend/src/test/java/com/khaleo/flashcard/integration/auth/PasswordResetRevokesSessionsIT.java
- [X] T041 [P] [US3] Add unit tests for lockout counter transitions in backend/src/test/java/com/khaleo/flashcard/unit/auth/LoginLockoutServiceTest.java

### Implementation for User Story 3

- [X] T042 [P] [US3] Implement forgot-password and reset-password DTOs in backend/src/main/java/com/khaleo/flashcard/controller/auth/dto/ForgotPasswordRequest.java, backend/src/main/java/com/khaleo/flashcard/controller/auth/dto/ResetPasswordRequest.java, and backend/src/main/java/com/khaleo/flashcard/controller/auth/dto/ResetPasswordResponse.java
- [X] T043 [US3] Implement failed-login counter, lockout trigger, and unlock handling in backend/src/main/java/com/khaleo/flashcard/service/auth/LoginLockoutService.java
- [X] T044 [US3] Integrate lockout checks and counter updates into login flow in backend/src/main/java/com/khaleo/flashcard/service/auth/AuthenticationService.java
- [X] T045 [US3] Implement password reset token issuance and email dispatch flow in backend/src/main/java/com/khaleo/flashcard/service/auth/PasswordResetService.java
- [X] T046 [US3] Implement password reset completion and full refresh-token revocation in backend/src/main/java/com/khaleo/flashcard/service/auth/PasswordResetService.java
- [X] T047 [US3] Implement forgot-password and reset-password endpoints in backend/src/main/java/com/khaleo/flashcard/controller/auth/AuthPasswordController.java
- [X] T048 [US3] Add auth observability events for lockout and password reset lifecycle outcomes in backend/src/main/java/com/khaleo/flashcard/service/auth/AuthAuditLogger.java

**Checkpoint**: User Story 3 is independently functional and testable.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Finalize documentation, observability validation, and quality evidence across all stories.

- [X] T049 [P] Update auth design artifacts and final decisions in specs/002-auth-security-verification/research.md and specs/002-auth-security-verification/data-model.md
- [X] T050 [P] Finalize auth API contract details and error codes in specs/002-auth-security-verification/contracts/auth-contract.md
- [X] T051 Add CloudWatch auth-related alarm definitions or updates in infra/terraform/cloudwatch-auth-security-alarms.tf
- [X] T052 Add New Relic auth-path instrumentation hooks in backend/src/main/java/com/khaleo/flashcard/config/observability/NewRelicAuthInstrumentation.java
- [X] T053 Execute full backend test suite and capture report artifacts in backend/build/reports/tests/phase6-test-and-coverage-summary.md
- [X] T054 Run quickstart verification steps and align command/output notes in specs/002-auth-security-verification/quickstart.md
- [X] T055 Complete constitutional compliance and checklist status update in specs/002-auth-security-verification/checklists/requirements.md

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Setup)**: No dependencies.
- **Phase 2 (Foundational)**: Depends on Phase 1 and blocks all user stories.
- **Phase 3 (US1)**: Depends on Phase 2 and delivers MVP onboarding/verification.
- **Phase 4 (US2)**: Depends on Phase 2 and uses foundational auth infrastructure plus verified-account behavior.
- **Phase 5 (US3)**: Depends on Phase 2 and integrates with US2 login/session flows for lockout and reset invalidation.
- **Phase 6 (Polish)**: Depends on completion of desired user stories.

### User Story Dependencies

- **US1 (P1)**: Starts after foundational phase; no dependency on other user stories.
- **US2 (P2)**: Starts after foundational phase; can be tested independently using pre-verified seed user.
- **US3 (P3)**: Starts after foundational phase; interacts with login/session paths from US2 but remains independently testable.

### Within Each User Story

- Write tests first and verify they fail before implementation.
- DTOs/models before service logic.
- Service logic before controller endpoint wiring.
- Add logging/observability and compliance updates before story checkpoint.

---

## Parallel Execution Examples

### User Story 1

```bash
Task: "T017 Add contract test for POST /api/v1/auth/register and GET /api/v1/auth/verify in backend/src/test/java/com/khaleo/flashcard/contract/auth/AuthRegistrationVerificationContractTest.java"
Task: "T018 Add integration test for register-to-verify flow in backend/src/test/java/com/khaleo/flashcard/integration/auth/AuthRegistrationVerificationIT.java"
Task: "T021 Implement registration request/response DTOs in backend/src/main/java/com/khaleo/flashcard/controller/auth/dto/"
Task: "T022 Implement verification endpoint request/response DTOs in backend/src/main/java/com/khaleo/flashcard/controller/auth/dto/"
```

### User Story 2

```bash
Task: "T027 Add contract test for POST /api/v1/auth/login, /refresh, and /logout in backend/src/test/java/com/khaleo/flashcard/contract/auth/AuthTokenLifecycleContractTest.java"
Task: "T028 Add integration test for login-refresh-logout lifecycle in backend/src/test/java/com/khaleo/flashcard/integration/auth/AuthTokenLifecycleIT.java"
Task: "T030 Add unit tests for JWT claims and expiry handling in backend/src/test/java/com/khaleo/flashcard/unit/auth/JwtTokenServiceTest.java"
Task: "T031 Implement login/refresh/logout DTOs in backend/src/main/java/com/khaleo/flashcard/controller/auth/dto/"
```

### User Story 3

```bash
Task: "T038 Add integration test for failed-login lockout threshold and duration in backend/src/test/java/com/khaleo/flashcard/integration/auth/LoginLockoutPolicyIT.java"
Task: "T039 Add integration test for forgot-password and reset flow with token expiry in backend/src/test/java/com/khaleo/flashcard/integration/auth/PasswordResetFlowIT.java"
Task: "T041 Add unit tests for lockout counter transitions in backend/src/test/java/com/khaleo/flashcard/unit/auth/LoginLockoutServiceTest.java"
Task: "T042 Implement forgot-password and reset-password DTOs in backend/src/main/java/com/khaleo/flashcard/controller/auth/dto/"
```

---

## Implementation Strategy

### MVP First (US1 Only)

1. Complete Setup and Foundational phases.
2. Complete US1 registration and verification flow.
3. Validate US1 independently (register, blocked unverified login, verify success, token single-use).
4. Pause for MVP demo/review.

### Incremental Delivery

1. Deliver US1 for onboarding and identity proofing.
2. Deliver US2 for session lifecycle (login, refresh, logout).
3. Deliver US3 for abuse resistance and account recovery.
4. Finish with Polish for observability and compliance evidence.

### Parallel Team Strategy

1. Team completes Phases 1-2 together.
2. After foundation:
   - Engineer A: US1 registration/verification.
   - Engineer B: US2 token lifecycle.
   - Engineer C: US3 lockout/reset.
3. Run merged regression and observability verification in Phase 6.

---

## Notes

- Task format strictly follows: `- [ ] T### [P] [US#] Description with file path`.
- `[P]` marker is used only when tasks can proceed independently on separate files.
- All stories include explicit tests because this feature changes authentication, security behavior, persistence, and runtime integrations.
- No pagination task is included because this feature introduces no list endpoints.
