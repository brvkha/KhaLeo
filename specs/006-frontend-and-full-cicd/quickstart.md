# Quickstart: Frontend + Full Auto CI/CD

## 1. Prerequisites

- AWS account with S3, CloudFront, EC2, SSM access.
- Existing backend infrastructure from previous specs.
- GitHub repository with Actions enabled.
- Java 17 and Node.js 20 for local validation.

## 2. Required GitHub Environment Secrets (`production`)

Create these environment secrets in GitHub `production`.
This deployment model uses OIDC role assumption only (no long-lived AWS access keys).

- `AWS_ROLE_TO_ASSUME`
- `AWS_REGION` (example: `ap-southeast-1`)
- `ARTIFACT_BUCKET` (backend jar artifacts)
- `FRONTEND_S3_BUCKET` (frontend static hosting)
- `CLOUDFRONT_DISTRIBUTION_ID`
- `DEPLOY_TARGET_TAG_KEY` (example: `Role`)
- `DEPLOY_TARGET_TAG_VALUE` (example: `khaleo-backend`)
- `DEPLOY_SERVICE_NAME` (systemd service name on EC2, example: `flashcard-backend`)

## 3. Terraform Output to Secret Mapping

Use the latest Terraform apply outputs as the source of truth.

| Terraform Output | GitHub Secret | Current Value |
|---|---|---|
| `github_actions_role_arn` | `AWS_ROLE_TO_ASSUME` | `arn:aws:iam::817888697629:role/github-actions-khaleo-prod-role` |
| `artifact_bucket` | `ARTIFACT_BUCKET` | `kha-leo-build-artifacts` |
| `frontend_bucket` | `FRONTEND_S3_BUCKET` | `khaleo-frontend-prod` |
| `deploy_target_tag.key` | `DEPLOY_TARGET_TAG_KEY` | `Role` |
| `deploy_target_tag.value` | `DEPLOY_TARGET_TAG_VALUE` | `khaleo-backend` |
| _(fixed value)_ | `AWS_REGION` | `ap-southeast-1` |

Set these placeholders manually if not yet known:

- `CLOUDFRONT_DISTRIBUTION_ID`
- `DEPLOY_SERVICE_NAME`

## 4. Required GitHub Variables (recommended)

- `JAVA_VERSION=17`
- `NODE_VERSION=20`
- `BACKEND_BASE_URL=https://api.your-domain.com`
- `APP_ENV=production`

## 5. Minimum IAM Permissions for CI/CD Role

Grant the IAM role assumed by GitHub Actions via OIDC these capabilities:

- S3 object read/write for artifact bucket and frontend bucket.
- CloudFront invalidation for the target distribution.
- SSM SendCommand + List/Get command invocation results.
- EC2 DescribeInstances for deploy target discovery by tag.

## 6. Workflow Model

- Run `ci.yml` for quality gates.
- Trigger deploy workflows manually via `workflow_dispatch` after CI success:
  - `deploy-backend.yml`
  - `deploy-frontend.yml`
- Both deploy workflows require `production` environment approval.

## 7. Push-to-Deploy Validation Checklist

1. Push a small frontend change to `main`.
2. Confirm CI passes for backend and frontend.
3. Confirm frontend deploy uploads built assets to S3.
4. Confirm CloudFront invalidation completes.
5. Confirm backend deploy publishes commit-SHA jar and runs SSM rollout.
6. Confirm application health endpoint returns success.
7. Run route-policy smoke checks with fixture accounts from `../008-public-deck-clone/test-data.md`.
8. Verify guest can open `/flashcard/decks` (and `/decks` alias), and protected routes redirect to login with return target.

### 7.1 Latest Validation Record

Date: 2026-03-17  
Feature branch: `006-frontend-and-full-cicd`

| Check | Status | Evidence |
|---|---|---|
| Frontend unit/component tests | PASS | `npm run test` (vitest: 6 passed) |
| Frontend production build | PASS | `npm run build` |
| CI coverage gate configured | PASS | `.github/workflows/ci.yml` uses `npm run test:coverage` |
| Backend deploy diagnostics and rollback hint | PASS | `.github/workflows/deploy-backend.yml` summary + rollback section |
| Frontend deploy diagnostics and rollback hint | PASS | `.github/workflows/deploy-frontend.yml` failure summary + rollback hint |
| End-to-end gated deployment run | PENDING | Requires manual `workflow_dispatch` runs with `production` approval |

Action pending before release:

- Execute real manual deploy runs in GitHub Actions and attach run URLs to this section.

## 8. Rollback (By Commit SHA)

1. Select a known-good commit SHA from Git history.
2. Trigger `Deploy Backend` via `workflow_dispatch` and set `artifactSha=<known-good-sha>`.
3. Approve `production` environment gate when prompted.
4. Trigger `Deploy Frontend` via `workflow_dispatch` with the same `artifactSha`.
5. Verify backend health endpoint and static frontend content.
6. Log rollback reason, SHA, and timestamp in release notes.

Important:

- Both deploy workflows must use the same SHA to keep backend/frontend versions consistent.
- No automatic rollback is expected on partial failures; use manual rollback flow above.

## 9. Common Failure Cases

- Missing secret: workflow fails at credential step.
- Wrong bucket/distribution IDs: deploy succeeds partially but site does not update.
- ASG/launch-template mismatch: backend immutable rollout fails during AMI bake or instance refresh.
- IAM deny on CloudFront invalidation: frontend files upload but stale cache persists.
