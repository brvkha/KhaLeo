# Feature Specification: Enterprise AWS Infrastructure and Full Terraform/GitHub Actions Automation

**Feature Branch**: `007-enterprise-aws-infra-cicd`
**Created**: 2026-03-17
**Status**: Ready for Planning
**Input**: User-provided project constitution and target AWS production flow for khaleoshop.click.

## Goal

Implement a fully automated infrastructure and deployment platform where:

1. CI and deploy workflows are executed with explicit operator control and approval.
2. Approved production deployment applies immutable backend and frontend artifacts.
3. Terraform is the source of truth for AWS + GitHub deployment contracts.
4. DNS for `khaleoshop.click` routes users to CloudFront (frontend) and ALB (backend API).

## Target Architecture

- **Frontend Path**: Route53 (`khaleoshop.click`) -> CloudFront -> S3 static bucket.
- **Backend Path**: Route53 (`api.khaleoshop.click`) -> WAF -> ALB (public subnets) -> EC2 Auto Scaling Group in private subnets across `ap-southeast-1a`, `ap-southeast-1b`, `ap-southeast-1c`.
- **Runtime**: EC2 runs Spring Boot artifact via immutable image rollout (Packer-baked AMI and Auto Scaling instance refresh), with commit-SHA traceability.
- **Data**: Aurora MySQL (core), DynamoDB (study activity logs).
- **Media**: S3 presigned uploads for image/audio.
- **Email**: SES for verification/reset.
- **Observability**: CloudWatch alarms, Splunk HEC JSON logs, New Relic APM.

## Clarifications

### Session 2026-03-17

- Q: Deployment auth model for GitHub Actions? -> A: OIDC role assumption only, no long-lived AWS keys.
- Q: Should production deploy be gated? -> A: Yes, GitHub `production` environment approval is mandatory.
- Q: Rollback model? -> A: Manual rollback by immutable commit SHA.
- Q: Terraform scope? -> A: Manage infra contracts and deploy dependencies; app runtime secrets remain in Secrets Manager and are rotated operationally.
 - Q: Preferred backend deployment runtime/format? -> A: SSM + immutable JAR (upload artifact to S3; use `ssm send-command` to copy jar to instances and restart `systemd`) — minimal repo changes, matches existing workflows and provides immutable artifacts.

## User Scenarios and Testing

### User Story 1 - One-Push Release Automation (Priority: P1)

As a release owner, I can run CI and CD workflows with approval gates so releases are predictable, auditable, and controlled.

**Independent Test**: Run CI workflow, then trigger deploy workflows via `workflow_dispatch`, approve production environment, and verify backend/frontend deploy complete with run summary.

**Acceptance Scenarios**:

1. **Given** CI workflow runs, **When** quality gates pass, **Then** deploy workflows are eligible for approved execution.
2. **Given** deploy approval is granted, **When** CD starts, **Then** backend artifact is rolled out to EC2 targets and frontend is synced to S3 with CloudFront invalidation.
3. **Given** deployment fails on any backend target, **When** workflow completes, **Then** run is failed with target-specific diagnostics.

### User Story 2 - Domain and Network Reliability (Priority: P1)

As a platform operator, I can route traffic via Route53 with resilient network topology across AZs so service remains available under instance or AZ disruption.

**Independent Test**: Verify DNS records resolve correctly, ALB target group healthy across multiple instances, and frontend domain serves CloudFront content.

**Acceptance Scenarios**:

1. **Given** Terraform apply completed, **When** DNS is queried, **Then** `khaleoshop.click` points to CloudFront and `api.khaleoshop.click` points to ALB.
2. **Given** one backend instance becomes unhealthy, **When** ALB health checks run, **Then** traffic is served from remaining healthy instances.
3. **Given** WAF rule blocks abusive traffic, **When** attack pattern occurs, **Then** malicious requests are denied before reaching backend.

### User Story 3 - Secure Runtime Configuration (Priority: P1)

As a security owner, I can keep runtime credentials in AWS Secrets Manager and use least-privilege IAM so production is secure by default.

**Independent Test**: Validate app instances can read required secrets, GitHub Actions role has only deploy permissions, and no static AWS keys exist in GitHub.

**Acceptance Scenarios**:

1. **Given** backend deployment starts, **When** container boots, **Then** DB/JWT/SES secrets are loaded from Secrets Manager through EC2 instance role permissions.
2. **Given** GitHub OIDC role trust policy, **When** token claims do not match repo/environment, **Then** role assumption is denied.
3. **Given** a workflow run, **When** AWS API calls are made, **Then** only permitted resources/actions succeed.

## Requirements

### Functional Requirements

- **FR-001**: Terraform MUST provision VPC with at least 3 private app subnets (1a/1b/1c) and public subnets for ALB/NAT as required.
- **FR-002**: Terraform MUST provision an ALB with HTTPS listener, target group health checks, and attachment to backend Auto Scaling Group.
- **FR-003**: Terraform MUST provision backend compute on EC2 with launch template/user data for Docker runtime and SSM agent compatibility.
- **FR-004**: Terraform MUST provision Route53 DNS records for `khaleoshop.click` (frontend) and `api.khaleoshop.click` (backend).
- **FR-005**: Terraform MUST provision CloudFront + S3 static site origin for frontend delivery.
- **FR-006**: Terraform MUST provision WAF association for ALB and baseline rate-based protections.
- **FR-007**: Terraform MUST provision Aurora MySQL and security groups limiting inbound to backend app layer only.
- **FR-008**: Terraform MUST provision or reference DynamoDB table for study activity logs.
- **FR-009**: Terraform MUST bootstrap GitHub `production` environment contracts (required secrets placeholders/variables where applicable).
- **FR-010**: GitHub Actions MUST use OIDC role assumption and MUST NOT use long-lived AWS access keys.
- **FR-011**: Backend CD MUST deploy immutable artifacts tagged by commit SHA (including immutable AMI rollout) and support manual rollback by SHA.
- **FR-012**: Frontend CD MUST sync built assets to S3 and invalidate CloudFront paths.
- **FR-013**: Production deployment MUST require manual approval through GitHub Environment protection rules.
- **FR-014**: Runtime app secrets MUST be read from AWS Secrets Manager, not committed to repository.
- **FR-015**: CI/CD runs MUST publish actionable summaries including commit SHA, target status, and rollback guidance.

### Non-Functional Requirements

- **NFR-001**: Support expected load baseline: 50 users, 30 concurrent users.
- **NFR-002**: Maintain test pyramid target 80% (Unit 60%, Integration 30%, E2E 10%).
- **NFR-003**: Frontend global cache invalidation after deploy should complete within operational SLO window.
- **NFR-004**: Deploy pipelines should fail fast on contract/permission mismatches.
- **NFR-005**: Backend logs must remain JSON-structured for Splunk ingestion.

## Required GitHub Environment Contract (`production`)

### Secrets

- `AWS_ROLE_TO_ASSUME`
- `AWS_REGION`
- `ARTIFACT_BUCKET`
- `FRONTEND_S3_BUCKET`
- `CLOUDFRONT_DISTRIBUTION_ID`
- `DEPLOY_TARGET_TAG_KEY`
- `DEPLOY_TARGET_TAG_VALUE`
- `DEPLOY_SERVICE_NAME`

### Variables

- `BACKEND_BASE_URL`
- `APP_ENV`
- `JAVA_VERSION`
- `NODE_VERSION`

## Success Criteria

- **SC-001**: 100% documented CI and deploy workflows can be executed with expected approval gating and immutable artifact traceability.
- **SC-002**: 100% production deploy runs require approval and use OIDC short-lived credentials.
- **SC-003**: 100% successful deploy runs publish backend and frontend with immutable version traceability.
- **SC-004**: DNS and TLS route both frontend and backend subdomain correctly.
- **SC-005**: No long-lived AWS credentials are stored in repository or GitHub Secrets for deployment authentication.
