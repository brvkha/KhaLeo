# Implementation Plan: Enterprise AWS Infrastructure and Full Terraform/GitHub Actions Automation

**Branch**: `feature/enterprise-aws-infra-cicd` | **Date**: 2026-03-17 | **Spec**: `specs/feature/enterprise-aws-infra-cicd/spec.md`
**Input**: Feature specification from `specs/feature/enterprise-aws-infra-cicd/spec.md`

## Summary

Implement an enterprise-grade AWS delivery path where CI and deploy workflows are explicitly run with production approval gates, and both frontend and backend releases are immutable and traceable. Frontend deploys to S3+CloudFront; backend deploys via immutable AMI rollout and ASG instance refresh behind ALB/WAF/Route53.

## Technical Context

**Language/Version**: Java 17 (Spring Boot backend), TypeScript + React 19 (frontend), Terraform >= 1.2, GitHub Actions YAML  
**Primary Dependencies**: Spring Boot, Hibernate, Flyway, React, Tailwind CSS, Vitest/Playwright, AWS provider (`hashicorp/aws`), GitHub provider (`integrations/github`)  
**Storage**: Aurora MySQL (core relational), DynamoDB (study activity/event logs), S3 (frontend artifacts, backend immutable artifacts, media uploads), Secrets Manager (runtime secrets)  
**Testing**: Maven test for backend, Vitest/Playwright for frontend, Terraform validate/plan in CI  
**Target Platform**: AWS in `ap-southeast-1`, GitHub-hosted Linux runners for CI/CD
**Project Type**: Web application (frontend + backend + IaC)  
**Performance Goals**: Support 50 expected users, 30 concurrent users; CI target <= 15 minutes; frontend initial load P75 < 3s  
**Constraints**: OIDC-only deploy authentication, production approval gate required, no auto-rollback, immutable artifact-by-SHA semantics, Terraform as infrastructure source of truth  
**Scale/Scope**: Production-ready single-region enterprise topology with 3-AZ private backend, public ALB edge, CloudFront static frontend, and full deployment governance

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- Architecture gate: PASS. Maintains replicated monolith and mandated stack (React/Tailwind + Java17/Spring Boot/Hibernate/Flyway + Aurora + DynamoDB).
- Infrastructure gate: PASS. All infrastructure changes planned in `infra/terraform/` modules; no imperative infra drift plan.
- Security gate: PASS. Uses OIDC role assumption, Secrets Manager runtime secrets, SSM deployment, WAF+ALB edge protections, no long-lived AWS access keys.
- API gate: PASS. No REST contract regression in this feature; pagination requirements unchanged and enforced by existing constitution.
- Observability gate: PASS. Plan keeps JSON logging, Splunk HEC, New Relic, and CloudWatch alarm coverage as explicit deliverables.
- Data gate: PASS. Aurora and Dynamo responsibilities remain separated; no Flyway contract violation.
- Quality gate: PASS with action. CI must include backend/frontend gates plus Terraform validate/plan and preserve 80% test target.
- Compliance gate: PASS. No direct FSRS v6 algorithm changes; study constraints remain unaffected.

## Project Structure

### Documentation (this feature)

```text
specs/feature/enterprise-aws-infra-cicd/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── cicd-infra-contract.md
└── tasks.md
```

### Source Code (repository root)

```text
backend/
├── src/main/java/
├── src/main/resources/
└── scripts/

frontend/
├── src/
└── tests/

infra/terraform/
├── main.tf
├── variables.tf
├── providers.tf
├── outputs.tf
└── *.tf (network/security/alarms/data modules)

.github/workflows/
├── ci.yml
├── deploy-backend.yml
└── deploy-frontend.yml
```

**Structure Decision**: Keep existing mono-repo with dedicated `backend`, `frontend`, and `infra/terraform` directories; implement new topology and pipeline hardening through additive Terraform/workflow modules without changing product runtime boundaries.

## Complexity Tracking

No constitution violations requiring justification.

## Post-Design Constitution Check

- Architecture gate: PASS. Design artifacts preserve replicated monolith, required stack, and AWS topology intent.
- Infrastructure gate: PASS. Contracts and quickstart keep Terraform as only infrastructure change mechanism.
- Security gate: PASS. OIDC trust scope, production approval gate, and Secrets Manager runtime model are explicitly enforced.
- API gate: PASS. This feature affects deployment/infrastructure; no API pagination contract regression introduced.
- Observability gate: PASS. Delivery contract requires deployment summaries and existing CloudWatch/Splunk/New Relic posture remains intact.
- Data gate: PASS. Aurora/Dynamo boundaries remain unchanged and documented in data model.
- Quality gate: PASS with implementation follow-up. CI gates and Terraform validate/plan are defined for task execution.
- Compliance gate: PASS. No FSRS v6, card-state, or daily-learning-limit rule changes in this scope.

## Final Compliance Checklist

- [X] Stack remains within constitutional boundaries (React/Tailwind, Spring Boot 17, Terraform, Aurora/Dynamo/S3/Secrets Manager).
- [X] Deploy authentication uses OIDC only; no long-lived AWS deployment credentials are referenced in workflows.
- [X] Production deployment remains approval-gated through GitHub `production` environment.
- [X] Runtime secret access is delegated to EC2 instance role with scoped `secretsmanager:GetSecretValue` access.
- [X] CloudFront, WAF, ALB, Route53, and multi-AZ backend topology are modeled in Terraform.
- [X] CI includes workflow contract checks, Terraform validation, backend tests, and frontend tests.
- [ ] Live quickstart apply/deploy execution evidence captured with AWS/GitHub production credentials.
