# Quickstart: Enterprise AWS Infra + CI/CD (Planning Output)

## 1. Configure Terraform Inputs

Create `infra/terraform/terraform.tfvars`:

```hcl
aws_region                 = "ap-southeast-1"
github_owner               = "brvkha"
github_repository          = "KhaLeo"
github_environment_name    = "production"
artifact_bucket            = "kha-leo-build-artifacts"
frontend_bucket            = "khaleo-frontend-prod"
route53_zone_id            = "<hosted-zone-id>"
alb_certificate_arn        = "<ap-southeast-1-acm-arn>"
cloudfront_acm_certificate_arn = "<us-east-1-acm-arn>"
deploy_target_tag_key      = "Role"
deploy_target_tag_value    = "khaleo-backend"
deploy_service_name        = "flashcard-backend"
db_secret_name             = "khaleo/prod/db-credentials"
jwt_secret_name            = "khaleo/prod/jwt-secret"
ses_secret_name            = "khaleo/prod/ses-credentials"
```

## 2. Provision Infrastructure Contracts

```bash
cd infra/terraform
terraform init
terraform validate
terraform plan -out tfplan
terraform apply tfplan
```

Capture outputs:
- `github_actions_role_arn`
- `github_actions_oidc_provider_arn`
- `artifact_bucket`
- `frontend_bucket`
- `github_environment_name`
- `secrets_manager_secrets`
- `cloudfront_distribution_id`
- `cloudfront_distribution_domain_name`
- `backend_alb_dns_name`

## 3. Verify Production Environment Contract

GitHub environment: `production`

Required secrets:
- `AWS_ROLE_TO_ASSUME`
- `AWS_REGION`
- `ARTIFACT_BUCKET`
- `FRONTEND_S3_BUCKET`
- `CLOUDFRONT_DISTRIBUTION_ID`
- `DEPLOY_TARGET_TAG_KEY`
- `DEPLOY_TARGET_TAG_VALUE`
- `DEPLOY_SERVICE_NAME`

Required variables:
- `BACKEND_BASE_URL` = `https://api.khaleoshop.click`
- `APP_ENV` = `production`
- `JAVA_VERSION` = `17`
- `NODE_VERSION` = `20`
- `DB_SECRET_ID` = `khaleo/prod/db-credentials`
- `JWT_SECRET_ID` = `khaleo/prod/jwt-secret`
- `SES_SECRET_ID` = `khaleo/prod/ses-credentials`
- `RUNTIME_ENV_PATH` = `/opt/khaleo/flashcard-backend/runtime-secrets.env`

## 4. Validate OIDC Trust Scope

Check IAM trust policy:
- `token.actions.githubusercontent.com:aud = sts.amazonaws.com`
- `token.actions.githubusercontent.com:sub = repo:<owner>/KhaLeo:environment:production`

## 5. Trigger Full Deployment

1. Push commit to `main`.
2. Wait for `CI` workflow success.
3. Confirm CI includes successful Terraform format/validate/plan checks.
4. Approve deploy in `production` environment.
5. Verify backend deploy summary reports a baked `AMI ID`, a successful `Instance refresh`, and target-group non-healthy targets = 0.
6. Verify frontend deploy summary includes CloudFront invalidation ID.

## 6. Post-Deploy Verification

- Frontend: `https://khaleoshop.click`
- Backend health: `https://api.khaleoshop.click/actuator/health`
- CloudWatch alarms and log streams available

## 6.1 US2 DNS and Topology Smoke Tests

1. Validate frontend DNS alias:
	- `nslookup khaleoshop.click`
	- Expect CloudFront-hosted target.
2. Validate API DNS alias:
	- `nslookup api.khaleoshop.click`
	- Expect ALB-hosted target.
3. Validate ALB target health:
	- In AWS console, open EC2 Target Groups and confirm healthy instances across three AZs.
4. Validate WAF association:
	- In WAF console, confirm `khaleo-backend-waf` is associated with backend ALB.
5. Validate API health through public edge:
	- `curl -i https://api.khaleoshop.click/actuator/health`
	- Expect successful health payload from ALB-routed backend.

## 7. Manual Rollback by SHA

1. Open GitHub Actions and run `Deploy Backend` via `workflow_dispatch`.
2. Set input `artifactSha=<known-good-sha>` and execute.
3. Confirm backend workflow bakes a known-good AMI and completes ASG instance refresh.
4. Run `Deploy Frontend` via `workflow_dispatch` with the same `artifactSha`.
5. Verify backend health endpoint and frontend smoke flow.
6. Capture both run summaries (SHA, AMI ID, instance refresh ID, invalidation ID) as rollback evidence.
