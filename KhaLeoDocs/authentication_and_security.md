# SPEC 002: Authentication, Security, and Identity Verification

## 1. Context & Objective
This specification defines the security perimeter for the "Kha Leo Flashcard" application. It implements Spring Security with a stateless JWT architecture, integrates AWS SES for email-based identity verification and password resets, and enforces strict rate-limiting policies to prevent abuse.

## 2. Technical Constraints (Referencing Constitution)
- **Framework:** Spring Security 6.x (Spring Boot 3.x compatible).
- **Session Management:** Strictly Stateless (`SessionCreationPolicy.STATELESS`). No HTTP Sessions.
- **Password Hashing:** BCryptPasswordEncoder.
- **AWS Integration:** Use AWS SDK for Java v2 (`software.amazon.awssdk:ses`) for sending emails. Do not use standard JavaMailSender with SMTP to minimize latency; use the AWS SES HTTP API.
- **Data Layer:** Utilize the `User` entity defined in Spec 001.

## 3. JWT Architecture
- **Access Token:** - Expiration: 15 minutes.
  - Claims: `userId`, `email`, `role`.
  - Transport: Client sends via `Authorization: Bearer <token>` header.
- **Refresh Token:**
  - Expiration: 7 days.
  - Storage: Must be stored in the database (create a `RefreshToken` entity: `id`, `token`, `user`, `expiryDate`) to allow token revocation.
  - Transport: Returned in the login payload or HttpOnly cookie.

## 4. Email Verification Flow (AWS SES)
- **Trigger:** When a `Guest` registers a new account, the `isEmailVerified` flag in the `User` entity is set to `false`.
- **Token Generation:** Generate a secure, URL-safe UUID verification token. Store this token in the database (create an `EmailVerificationToken` entity with an expiration of 24 hours).
- **Email Dispatch:** Construct an HTML email containing a verification link (e.g., `https://app.coursekhaleo.click/verify?token=XYZ`). Send via AWS SES client.
- **Verification Endpoint:** When the link is clicked, validate the token, set `isEmailVerified = true`, and delete the token.
- **Constraint:** Unverified users MUST NOT be able to log in or obtain a JWT.

## 5. Rate Limiting & Account Lockout
- **Rule:** Maximum 5 failed login attempts. Exceeding this locks the account for exactly 1 day (24 hours).
- **Implementation Strategy:** - Add `failedLoginAttempts` (Integer, default 0) and `accountLockedUntil` (Timestamp, nullable) to the `User` entity.
  - On failed login: Increment `failedLoginAttempts`. If it reaches 5, set `accountLockedUntil` to `now() + 24 hours`.
  - On successful login: Reset `failedLoginAttempts` to 0.
  - Login attempt must first check if `accountLockedUntil` is in the future. If yes, throw a `LockedException`.

## 6. Password Reset Flow
- **Trigger:** User requests a password reset by providing their email.
- **Process:** Generate a secure reset token (valid for 1 hour). Send via AWS SES.
- **Completion:** User submits the new password along with the token. Validate token, hash the new password using BCrypt, update the user record, and invalidate all existing Refresh Tokens for that user.

## 7. Required REST API Endpoints
All endpoints below should be under the `/api/v1/auth` prefix and must be entirely public (permit all).
1. `POST /register`: Accepts email, password. Returns 201 Created. Triggers SES email.
2. `GET /verify?token={token}`: Verifies the email token.
3. `POST /login`: Accepts email, password. Returns 200 OK with `{ accessToken, refreshToken, expiresIn }`. Enforces rate limiting.
4. `POST /refresh`: Accepts a valid Refresh Token. Returns a new Access Token.
5. `POST /forgot-password`: Accepts email. Triggers SES email.
6. `POST /reset-password`: Accepts token and new password.
7. `POST /logout`: Accepts Refresh Token and deletes it from the database (revocation).

## 8. Acceptance Criteria
1. `SecurityFilterChain` is configured to block all `/api/v1/**` requests unless authenticated, except for the `/api/v1/auth/**` whitelist.
2. `JwtAuthenticationFilter` intercepts requests, validates the Access Token, and populates the `SecurityContextHolder`.
3. Failed login attempts increment the counter in the database, and the account successfully locks out after the 5th attempt.
4. AWS SES successfully dispatches emails (mocked in Unit/Integration tests, real implementation relies on AWS credentials injected via environment variables).
5. Spring Security handles exceptions (401 Unauthorized, 403 Forbidden) gracefully, returning standardized JSON error responses instead of default HTML pages.

## 9. Execution Instructions for AI
Generate the necessary Spring Security configuration classes, JWT utility classes, Authentication Controllers, Services (including AWS SES service), and update the User entity/migrations to support the lockout and token tracking mechanisms.