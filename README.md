# KhaLeo

Flashcard learning platform with Spring Boot backend, React frontend, and AWS deployment automation.

## Release Readiness Checklist

Before approving production release:

- [ ] Backend CI checks pass (`mvn test`, package build).
- [ ] Frontend CI checks pass (`lint`, `test:coverage`, `build`).
- [ ] Deploy workflows use OIDC role and environment `production` approval gate.
- [ ] Artifact SHA selected for release and documented.
- [ ] Runtime secrets exist in AWS Secrets Manager (`khaleo/prod/*`).
- [ ] Rollback SHA is identified and confirmed deployable.
- [ ] Post-deploy health checks pass (`/actuator/health`, frontend smoke).

## Core Paths

- Backend: `backend/`
- Frontend: `frontend/`
- Terraform: `infra/terraform/`
- Feature specs: `specs/`

## Enterprise CI/CD Contract (Phase 1-2)

- Terraform is the source of truth for AWS deploy contracts and GitHub production environment configuration.
- CI (`.github/workflows/ci.yml`) runs Terraform format/validate/speculative plan plus backend/frontend quality gates.
- Deploy workflows must assume AWS role via OIDC and use immutable artifact-by-SHA paths.
- Backend deploy uses immutable AMI bake (artifact SHA -> AMI -> ASG instance refresh) so replacement EC2 instances always boot with a pre-baked app binary.
- Runtime secrets are sourced from AWS Secrets Manager (`khaleo/prod/*`) and never committed.

### Minimum Production Environment Values

Secrets:

- `AWS_ROLE_TO_ASSUME`
- `AWS_REGION`
- `ARTIFACT_BUCKET`
- `FRONTEND_S3_BUCKET`
- `CLOUDFRONT_DISTRIBUTION_ID`
- `DEPLOY_TARGET_TAG_KEY`
- `DEPLOY_TARGET_TAG_VALUE`
- `DEPLOY_SERVICE_NAME`

Variables:

- `BACKEND_BASE_URL`
- `APP_ENV`
- `JAVA_VERSION`
- `NODE_VERSION`
- `DB_SECRET_ID`
- `JWT_SECRET_ID`
- `SES_SECRET_ID`
- `RUNTIME_ENV_PATH`

## Release Evidence And Runbooks

- MVP validation evidence: `specs/feature/enterprise-aws-infra-cicd/mvp-validation.md`
- Quickstart validation status: `specs/feature/enterprise-aws-infra-cicd/quickstart-validation.md`
- Rollback runbook: `specs/feature/enterprise-aws-infra-cicd/quickstart.md#7-manual-rollback-by-sha`
