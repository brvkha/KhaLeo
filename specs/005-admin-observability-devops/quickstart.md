# Quickstart: Administration, Observability, and DevOps Pipeline

## 1. Prerequisites

- Java 17 installed.
- Maven available.
- Docker available for integration tests that require containerized dependencies.
- AWS credentials configured for Terraform and deployment workflow validation.
- Access to Splunk HEC token and New Relic license key via secure environment variables.

## 2. Implement Admin APIs

1. Add admin controller/service layer under backend/src/main/java/com/khaleo/flashcard/controller/admin and service/admin.
2. Implement endpoints:
   - GET /api/v1/admin/stats
   - POST /api/v1/admin/users/{userId}/ban
   - DELETE /api/v1/admin/decks/{deckId}
   - PUT /api/v1/admin/cards/{cardId}
3. Enforce admin role authorization on all admin endpoints.
4. Ensure ban action takes effect on every authenticated request immediately, including previously issued access tokens.

## 3. Implement Ban Enforcement and Auditability

1. Extend security/auth request pipeline to validate banned-user status on authenticated requests.
2. Add auditable moderation event emission for ban/delete/edit actions.
3. Keep unban API out of scope for this feature.

## 4. Implement Observability Integration

1. Update backend/src/main/resources/logback-spring.xml:
   - Keep structured JSON logs.
   - Add Splunk HEC appender wrapped by AsyncAppender.
2. Update backend/src/main/resources/application.yml with Splunk and New Relic configuration properties sourced from environment variables.
3. Ensure logging transport failures are observable and non-blocking for request execution.
4. Add/update relevant New Relic instrumentation usage where needed for admin and deployment-signaling events.

## 5. Implement CI/CD Deployment Workflow

1. Create .github/workflows/deploy-backend.yml.
2. Workflow steps:
   - Trigger on push to main.
   - Set up Java 17 and Maven.
   - Build jar.
   - Publish immutable commit-SHA artifact to S3.
   - Execute aws ssm send-command to tagged EC2 targets.
3. Enforce completion semantics:
   - Dispatch to all intended targets.
   - Mark workflow failed if any target fails.
4. Include per-target deployment result reporting for operator visibility.

## 6. Terraform and Infra Updates

1. Keep infra changes under infra/terraform only.
2. Add or update variables/alarms needed for new admin/deployment observability outcomes.
3. Validate terraform fmt and terraform plan produce expected additive changes.

## 7. Run Verification

From repository root:

```bash
cd backend
mvn -q -DskipTests flyway:validate
mvn test
```

Optional focused tests:

```bash
mvn -Dtest="*Admin*" test
mvn -Dtest="*Security*" test
```

## 8. Validate Critical Behaviors

- Non-admin principals cannot access /api/v1/admin endpoints.
- Ban action blocks all authenticated requests immediately for the banned account.
- Admin stats responses are no older than 5 minutes for the defined success target.
- Log events remain JSON-structured and are shipped asynchronously to Splunk.
- New Relic agent startup is active in deployed runtime.
- Deployment workflow publishes immutable SHA artifact and fails run if any target fails.

## 9. Evidence to Capture

- Contract/integration/unit test reports for admin authorization, moderation, and ban enforcement.
- Sample structured logs for admin actions and access denials.
- Workflow execution summary showing artifact SHA and per-target SSM results.
- Terraform diff for any alarm or deployment-supporting infra changes.

## 10. Execution Evidence (2026-03-16)

- Focused US2/US3 verification tests: `12 passed, 0 failed`.
- Full backend verification run: `155 passed, 0 failed`.
- Terraform validation:
   - `terraform fmt -recursive` completed.
   - `terraform plan -refresh=false -lock=false -input=false -no-color` completed with additive plan.
   - Plan highlights include new alarms:
      - `aws_cloudwatch_metric_alarm.admin_authorization_denials_high`
      - `aws_cloudwatch_metric_alarm.deployment_command_failure_high`
