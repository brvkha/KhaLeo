# Tasks: Enterprise AWS Infrastructure and Full Terraform/GitHub Actions Automation

**Input**: Design docs from `/specs/007-enterprise-aws-infra-cicd/`
**Prerequisites**: `spec.md`

## Phase 1: Foundation and Contracts

- [ ] T001 Validate Terraform provider/state strategy for multi-environment deployment in infra/terraform/
- [ ] T002 Define domain contract for `khaleoshop.click` + `api.khaleoshop.click` in infra/terraform/variables.tf
- [ ] T003 Define GitHub `production` secrets/vars mapping table in specs/007-enterprise-aws-infra-cicd/quickstart.md
- [ ] T004 Confirm OIDC trust policy scope to repository + environment claim in infra/terraform/main.tf

## Phase 2: Networking and Edge

- [ ] T005 Provision VPC, IGW, route tables, and 3 private app subnets across ap-southeast-1a/1b/1c in infra/terraform/network.tf
- [ ] T006 Provision ALB in public subnets with HTTPS listener and target group health checks in infra/terraform/alb.tf
- [ ] T007 Provision WAF web ACL with baseline managed rules + rate limiting and associate with ALB in infra/terraform/waf.tf
- [ ] T008 Provision Route53 records for frontend and backend hostnames in infra/terraform/route53.tf
- [ ] T009 Provision CloudFront distribution with S3 origin for frontend in infra/terraform/cloudfront.tf

## Phase 3: Compute and Data

- [ ] T010 Provision launch template + Auto Scaling Group for backend Docker runtime in infra/terraform/compute.tf
- [ ] T011 Provision security groups and IAM instance profile for EC2 access to SSM/S3/Secrets in infra/terraform/iam-ec2.tf
- [ ] T012 Provision Aurora MySQL cluster, subnet group, and app-only ingress rules in infra/terraform/aurora.tf
- [ ] T013 Ensure DynamoDB study activity table contract is wired into runtime config in infra/terraform/dynamodb-study-activity.tf
- [ ] T014 Provision/verify Secrets Manager entries for runtime config and rotation ownership in infra/terraform/main.tf

## Phase 4: CI/CD Automation

- [ ] T015 Harden CI workflow with strict quality gates and explicit fail-fast summaries in .github/workflows/ci.yml
- [ ] T016 Harden backend deploy workflow to rollout immutable SHA artifacts to EC2 targets with per-instance diagnostics in .github/workflows/deploy-backend.yml
- [ ] T017 Harden frontend deploy workflow for S3 sync + CloudFront invalidation + summary outputs in .github/workflows/deploy-frontend.yml
- [ ] T018 Add rollback-by-SHA dispatch flow and runbook enforcement checks in .github/workflows/deploy-backend.yml
- [ ] T019 Add pipeline guard to block deploy when production environment protections are missing in .github/workflows/deploy-*.yml

## Phase 5: Verification and Handover

- [ ] T020 Add Terraform validation/plan checks in CI for infrastructure drift detection in .github/workflows/ci.yml
- [ ] T021 Execute manual gated deployment rehearsal (CI + workflow_dispatch deploy) and capture evidence in specs/007-enterprise-aws-infra-cicd/quickstart.md
- [ ] T022 Add operational runbook for incident response and rollback in KhaLeoDocs/cicd_runbook_and_iam_policies.md
- [ ] T023 Update root release readiness checklist with infra automation requirements in README.md
- [ ] T024 Mark completed tasks and attach command evidence links in specs/007-enterprise-aws-infra-cicd/tasks.md

## Critical Path

- T001 -> T004 -> T015 -> T016 -> T018 -> T021 -> T024
- T002 -> T008 -> T021
- T005 -> T006 -> T010 -> T016
- T009 -> T017 -> T021

## Suggested MVP for This Spec

- MVP-A: T001, T002, T003, T004, T015, T016, T017, T021, T022
- MVP-B: Add T005, T006, T008, T009 to productionize edge/network path
