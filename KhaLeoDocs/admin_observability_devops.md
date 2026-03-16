# SPEC 005: Administration, Observability, and DevOps Pipeline

## 1. Context & Objective
This final specification implements the Admin management capabilities, configures enterprise-grade observability (Splunk JSON logging, New Relic APM), and defines the CI/CD pipeline structure for deploying the Spring Boot artifact to the AWS EC2 instances.

## 2. Technical Constraints
- **Observability:** `logback-spring.xml` must be configured to output purely JSON formatted logs.
- **CI/CD:** Use GitHub Actions. Deployment to EC2 must avoid direct SSH access. Use AWS Systems Manager (SSM) `send-command` to instruct EC2 instances in private subnets to pull the latest `.jar` from an S3 artifacts bucket.

## 3. Admin Dashboard & Management (APIs)
- **Role Requirement:** All endpoints under `/api/v1/admin/**` must require `ROLE_ADMIN`.
- `GET /api/v1/admin/stats`: Returns platform-wide analytics (Total registered users, total decks, total cards, total reviews logged in the last 24 hours).
- `POST /api/v1/admin/users/{userId}/ban`: Locks a user account permanently (or until unbanned) by updating the `User` entity.
- `DELETE /api/v1/admin/decks/{deckId}`: Hard delete any deck (even private ones) for content moderation.
- `PUT /api/v1/admin/cards/{cardId}`: Edit any card content to fix inaccuracies in public decks.

## 4. Observability: Splunk & New Relic
- **Splunk HEC Integration:** - Include the `splunk-library-javalogging` dependency.
  - Create a `logback-spring.xml` utilizing the `HttpEventCollectorLogbackAppender`.
  - Wrap the Splunk appender in an `<appender name="ASYNC_SPLUNK" class="ch.qos.logback.classic.AsyncAppender">` to prevent HTTP latency from blocking the main Spring Boot threads.
  - Use `<springProperty>` to map `SPLUNK_HEC_URL` and `SPLUNK_HEC_TOKEN` from `application.yml`.
- **New Relic APM:** Ensure the `Dockerfile` or EC2 startup script (if running bare metal jar) includes the `-javaagent:/path/to/newrelic.jar` flag. No code-level changes are required for New Relic.

## 5. CI/CD Pipeline (GitHub Actions)
- Provide a `.github/workflows/deploy-backend.yml` configuration.
- **Workflow Steps:**
  1. Trigger on `push` to `main`.
  2. Setup JDK 17 & Maven.
  3. Run `mvn clean package`.
  4. Configure AWS Credentials via GitHub Secrets (`AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`).
  5. Upload the generated `app.jar` to an S3 bucket (e.g., `s3://kha-leo-build-artifacts/app.jar`).
  6. Execute `aws ssm send-command` targeting the EC2 instances (via tags) to download the `.jar` from S3 and restart the `Systemd` service running the Spring Boot application.

## 6. Execution Instructions for AI
Create the Admin controllers and services with proper `@PreAuthorize("hasRole('ADMIN')")` checks. Provide the complete `logback-spring.xml` configuration for async Splunk logging. Generate the GitHub Actions YAML file that satisfies the SSM deployment requirements.

## 7. Runbook Notes

### Deploy Workflow Operations
- Workflow: `.github/workflows/deploy-backend.yml`.
- Trigger: push to `main` or manual dispatch.
- Artifact strategy: immutable `backend/<commit-sha>/app.jar` in S3.
- Targeting: SSM command dispatch by tag key/value.
- Completion semantics: dispatch to all intended targets, fail run if any target reports `Failed`, `TimedOut`, or `Cancelled`.

### Required Runtime Configuration
- `SPLUNK_ENABLED`
- `SPLUNK_HEC_URL`
- `SPLUNK_HEC_TOKEN`
- `NEW_RELIC_AGENT_ENABLED`
- `NEW_RELIC_APP_NAME`

### Operational Signals
- Admin authorization denials: metric `AdminAuthorizationDenied`.
- Deployment target failures: metric `DeploymentCommandFailure`.
- Admin moderation outcomes/failures: New Relic structured events from admin instrumentation helpers.