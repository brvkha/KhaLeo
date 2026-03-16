# Implementation Plan: Administration, Observability, and DevOps Pipeline

**Branch**: `005-admin-observability-devops` | **Date**: 2026-03-16 | **Spec**: `specs/005-admin-observability-devops/spec.md`
**Input**: Feature specification from `/specs/005-admin-observability-devops/spec.md`

**Note**: This template is filled in by the `/speckit.plan` command. See `.specify/templates/plan-template.md` for the execution workflow.

## Summary

Deliver admin moderation APIs and platform stats under strict admin-only access,
enforce immediate request-time ban checks for previously issued tokens, implement
asynchronous Splunk HEC JSON logging plus New Relic Java Agent runtime support,
and add an immutable commit-SHA GitHub Actions deployment pipeline that publishes
backend artifacts to S3 and deploys to tagged EC2 targets via AWS SSM with
"attempt all targets, fail if any target fails" semantics.

## Technical Context

**Language/Version**: Java 17 (Spring Boot 3.3.2), SQL (Aurora MySQL via Flyway), Terraform HCL, GitHub Actions YAML  
**Primary Dependencies**: Spring Web, Spring Security, Spring Data JPA/Hibernate, Flyway, AWS SDK v2 (S3/SES/DynamoDB), Splunk Java Logging Library, JUnit 5/Testcontainers  
**Storage**: Aurora MySQL for user/deck/card/admin state, DynamoDB for study activity logs, S3 for immutable backend artifacts  
**Testing**: JUnit 5 unit tests, Spring integration tests (Testcontainers), MockMvc contract tests, workflow dry-run/lint checks  
**Target Platform**: Dockerized Spring Boot backend on AWS EC2 private subnets behind ALB/WAF; GitHub-hosted runners for CI/CD
**Project Type**: Web application (replicated monolith backend + frontend + Terraform IaC)  
**Performance Goals**: 95% of admin moderation APIs <2s; 99% of required observability events available within 60s; 99% of admin stats freshness <=5 minutes lag  
**Constraints**: Ban enforcement must apply on every authenticated request including pre-issued tokens; no public unban API; deployment must dispatch to all targets and fail overall if any target fails; immutable commit-SHA artifact references for deploy/rollback determinism  
**Scale/Scope**: Portfolio target load (about 50 users, about 30 concurrent); feature scope includes admin APIs, security checks, logging/APM config, deployment workflow, and additive Terraform alarm/config adjustments

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

Initial Gate Assessment (Pre-Phase 0)

- Architecture gate: PASS. Keeps mandated Java 17 Spring Boot monolith and
  preserves Aurora/Dynamo/S3 responsibilities without topology deviation.
- Infrastructure gate: PASS. Infra changes stay Terraform-managed, including
  alarm/config updates and deployment prerequisites.
- Security gate: PASS. Plan includes admin-only authorization, request-time ban
  enforcement, and secret handling through environment/secret stores.
- API gate: PASS. Admin endpoints are RESTful and no list endpoint is added;
  pagination clause remains explicitly not applicable.
- Observability gate: PASS. Plan includes JSON logs to Splunk HEC, New Relic
  Java Agent runtime enablement, and CloudWatch alarm coverage updates.
- Data gate: PASS. Aurora remains source of truth for mutable admin/user state;
  no Flyway bypass is introduced.
- Quality gate: PASS. Test strategy covers unit/integration/contract layers and
  remains aligned with 80% and 60/30/10 quality policy.
- Compliance gate: PASS. Feature has no SM-2/card-state logic changes and
  explicitly preserves learning algorithm rules.

Post-Design Re-check (Post-Phase 1)

- Architecture gate: PASS. `research.md`, `data-model.md`, contracts, and
  quickstart keep work inside existing backend and infra boundaries.
- Infrastructure gate: PASS. Design artifacts route infra updates through
  `infra/terraform` and deployment changes through repository workflows/scripts.
- Security gate: PASS. Contracts require `ROLE_ADMIN` enforcement and immediate
  banned-user rejection for all authenticated requests.
- API gate: PASS. Admin API contract is RESTful and explicitly states no list
  endpoint was introduced by this feature.
- Observability gate: PASS. Contract and quickstart define required telemetry
  for admin actions, deployment outcomes, and log-shipping failure visibility.
- Data gate: PASS. Data model keeps moderation/account changes in Aurora with
  auditable action events and no violation of migration discipline.
- Quality gate: PASS. Design artifacts define test coverage across authorization,
  moderation behavior, logging integration, and deployment contract behavior.
- Compliance gate: PASS. Post-design artifacts state no impact on SM-2,
  card-state transitions, or account-level daily learning-limit logic.

## Project Structure

### Documentation (this feature)

```text
specs/005-admin-observability-devops/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── admin-observability-devops-contract.md
└── tasks.md             # Created later by /speckit.tasks
```

### Source Code (repository root)
```text
backend/
├── pom.xml
├── src/main/java/com/khaleo/flashcard/
│   ├── config/
│   │   ├── security/
│   │   └── observability/
│   ├── controller/
│   │   └── admin/
│   ├── service/
│   │   ├── admin/
│   │   ├── auth/
│   │   └── persistence/
│   ├── repository/
│   └── entity/
├── src/main/resources/
│   ├── application.yml
│   └── logback-spring.xml
└── src/test/java/com/khaleo/flashcard/
  ├── contract/
  ├── integration/
  └── unit/

infra/
└── terraform/
  ├── main.tf
  ├── variables.tf
  ├── cloudwatch-auth-security-alarms.tf
  └── cloudwatch-persistence-alarms.tf

.github/
└── workflows/
  └── deploy-backend.yml

KhaLeoDocs/
└── admin_observability_devops.md
```

**Structure Decision**: Use the existing web-application monolith structure.
Implement admin/security/observability changes under `backend/`, deployment and
alarm prerequisites under `infra/terraform`, and CI/CD orchestration in a new
GitHub Actions workflow under `.github/workflows`.

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|

No constitutional violations requiring justification.

## Observability Output Validation (Phase 6)

- Structured JSON logging: validated in `backend/src/test/java/com/khaleo/flashcard/contract/observability/LoggingConfigurationContractTest.java`.
- Splunk async non-blocking behavior: validated in `backend/src/test/java/com/khaleo/flashcard/integration/observability/SplunkAsyncNonBlockingIT.java`.
- Admin/deployment instrumentation signal coverage: validated in `backend/src/test/java/com/khaleo/flashcard/unit/observability/AdminObservabilityInstrumentationTest.java`.
- CloudWatch alarmable outcomes added:
  - `AdminAuthorizationDenied` via `infra/terraform/cloudwatch-auth-security-alarms.tf`.
  - `DeploymentCommandFailure` via `infra/terraform/cloudwatch-persistence-alarms.tf`.
