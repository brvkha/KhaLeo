# Implementation Plan: Authentication, Security, and Identity Verification

**Branch**: `002-auth-security-verification` | **Date**: 2026-03-16 | **Spec**: `specs/002-auth-security-verification/spec.md`
**Input**: Feature specification from `/specs/002-auth-security-verification/spec.md`

**Note**: This template is filled in by the `/speckit.plan` command. See `.specify/templates/plan-template.md` for the execution workflow.

## Summary

Deliver an end-to-end authentication and account security foundation for the
Kha Leo Flashcard backend with stateless JWT access/refresh flows, mandatory
email verification, password reset via expiring tokens, refresh-token
revocation, and deterministic login lockout after five failed attempts. The
approach extends the existing Aurora-backed user model with token lifecycle
entities, maintains REST contracts under `/api/v1/auth`, and adds auditable
security observability aligned with Splunk/New Relic/CloudWatch governance.

## Technical Context

**Language/Version**: Java 17 (Spring Boot backend), SQL (MySQL 8.0 via Flyway), Terraform HCL  
**Primary Dependencies**: Spring Security 6.x, Spring Boot Web/Validation,
Spring Data JPA/Hibernate, Flyway, AWS SDK v2 (SES), JWT library (JJWT or equivalent), JUnit 5/Testcontainers  
**Storage**: Aurora MySQL for users and auth-token state; DynamoDB unchanged for study activity logs  
**Testing**: JUnit 5 unit tests, Spring integration tests, contract tests for auth flows and migration constraints  
**Target Platform**: Dockerized Spring Boot service on AWS EC2 (private subnets) behind ALB/WAF, Terraform-managed  
**Project Type**: Web application (replicated monolith with backend + frontend + IaC)  
**Performance Goals**: Authentication endpoints respond under 500 ms p95 at expected load (50 users, 30 concurrent);
verification/reset email dispatch initiated within 60 seconds for 95% of requests  
**Constraints**: Access token TTL 15 minutes; refresh token TTL 7 days; lockout exactly 24 hours after 5 consecutive failures;
no HTTP session state; Flyway-only relational schema changes; no disclosure of account existence in forgot-password path  
**Scale/Scope**: Backend auth module and persistence extensions covering registration, verification, sign-in,
refresh, reset, logout, and security telemetry for initial portfolio scale

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

Initial Gate Assessment (Pre-Phase 0)

- Architecture gate: PASS. Feature stays within Java 17 + Spring Boot monolith
  and Aurora/DynamoDB split; no stack deviations.
- Infrastructure gate: PASS. SES usage and existing AWS resources remain
  Terraform-governed; no manual infra changes assumed.
- Security gate: PASS. Requirements explicitly preserve JWT 15m/7d, SES
  verification, and 5-failure 24h lockout mandates; secrets remain externalized.
- API gate: PASS. Feature exposes RESTful auth endpoints; no list endpoints are
  introduced so pagination requirement is explicitly not applicable.
- Observability gate: PASS. Plan includes structured security events,
  unauthorized/forbidden outcomes, token lifecycle traces, and alarm impact.
- Data gate: PASS. Aurora responsibilities are explicit for users and auth-token
  records; Flyway migration requirements are included.
- Quality gate: PASS. Testing strategy is mapped to unit/integration/contract
  coverage with expected alignment to 80% and 60/30/10 targets.
- Compliance gate: PASS. Feature has no SM-2 or card-state behavior changes and
  preserves account-level learning-limit rules.

Post-Design Re-check (Post-Phase 1)

- Architecture gate: PASS. `research.md` and `data-model.md` retain monolith
  boundaries and approved backend technologies.
- Infrastructure gate: PASS. `quickstart.md` and `contracts/auth-contract.md`
  keep SES/credentials and any alarm adjustments in Terraform-managed flows.
- Security gate: PASS. Design artifacts enforce stateless auth, verification
  prerequisite, bounded lockout, token expiry/revocation, and reset-session invalidation.
- API gate: PASS. Contract defines all required auth endpoints and expected
  response/error behavior with no list endpoints.
- Observability gate: PASS. Security-event logging and telemetry points are
  documented for auth runtime paths.
- Data gate: PASS. New relational entities and migration expectations are
  clearly bounded to Aurora; DynamoDB remains unaffected.
- Quality gate: PASS. Artifact set defines layered automated tests for auth
  success/failure paths and lockout/reset determinism.
- Compliance gate: PASS. No algorithm fidelity regressions and no daily learning
  limit behavioral changes are introduced.

## Project Structure

### Documentation (this feature)

```text
specs/002-auth-security-verification/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── auth-contract.md
└── tasks.md             # Created later by /speckit.tasks
```

### Source Code (repository root)

```text
backend/
├── src/main/java/com/khaleo/flashcard/
│   ├── config/
│   ├── entity/
│   ├── repository/
│   ├── service/
│   └── controller/      # expected auth controller location in implementation
├── src/main/resources/
│   └── db/migration/
└── src/test/java/com/khaleo/flashcard/
    ├── unit/
    ├── integration/
    └── contract/

frontend/
└── src/

infra/
└── terraform/
```

**Structure Decision**: Use the existing web-application repository structure,
implementing authentication behavior in `backend/` while preserving current
`frontend/` and `infra/terraform/` boundaries.

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

No constitutional violations requiring justification.
