# Quickstart: Frontend + Full Auto CI/CD

## 1. Prerequisites

- AWS account with S3, CloudFront, EC2, SSM access.
- Existing backend infrastructure from previous specs.
- GitHub repository with Actions enabled.
- Java 17 and Node.js 20 for local validation.

## 2. Required GitHub Secrets

Create these repository or environment secrets:

- `AWS_ACCESS_KEY_ID`
- `AWS_SECRET_ACCESS_KEY`
- `AWS_REGION` (example: `ap-southeast-1`)
- `ARTIFACT_BUCKET` (backend jar artifacts)
- `FRONTEND_S3_BUCKET` (frontend static hosting)
- `CLOUDFRONT_DISTRIBUTION_ID`
- `DEPLOY_TARGET_TAG_KEY` (example: `Service`)
- `DEPLOY_TARGET_TAG_VALUE` (example: `khaleo-backend`)
- `DEPLOY_SERVICE_NAME` (systemd service name on EC2, example: `khaleo-backend`)

## 3. Required GitHub Variables (recommended)

- `JAVA_VERSION=17`
- `NODE_VERSION=20`
- `BACKEND_BASE_URL=https://api.your-domain.com`
- `APP_ENV=production`

## 4. Minimum IAM Permissions for CI/CD User

Grant the IAM principal used by GitHub Actions these capabilities:

- S3 object read/write for artifact bucket and frontend bucket.
- CloudFront invalidation for the target distribution.
- SSM SendCommand + List/Get command invocation results.
- EC2 DescribeInstances for deploy target discovery by tag.

## 5. Branch and Workflow Model

- PR to `main`: run `ci.yml` only (test/build checks).
- Push/Merge to `main`: run `ci.yml`, then deploy workflows:
  - `deploy-backend.yml`
  - `deploy-frontend.yml`

## 6. Push-to-Deploy Validation Checklist

1. Push a small frontend change to `main`.
2. Confirm CI passes for backend and frontend.
3. Confirm frontend deploy uploads built assets to S3.
4. Confirm CloudFront invalidation completes.
5. Confirm backend deploy publishes commit-SHA jar and runs SSM rollout.
6. Confirm application health endpoint returns success.

## 7. Rollback (By Commit SHA)

- Re-run workflow dispatch with a previous known-good SHA for both frontend and backend.
- Verify deployment and health checks.
- Record rollback event in release notes.

## 8. Common Failure Cases

- Missing secret: workflow fails at credential step.
- Wrong bucket/distribution IDs: deploy succeeds partially but site does not update.
- SSM target tags mismatch: backend rollout dispatches to zero instances.
- IAM deny on CloudFront invalidation: frontend files upload but stale cache persists.
