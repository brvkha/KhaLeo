# KhaLeo Backend Feature Runbook

## Scope

Backend scope for feature 008 includes:

- Public deck discovery and authenticated import/re-import merge.
- Private workspace ownership boundaries for Study and Cards flows.
- Study session scheduling with new-card first-step timing policy.
- Observability instrumentation and persistence quality gates.

## Local Commands

```bash
mvn test
mvn "-Dtest=PublicDeck*" test
mvn "-Dtest=*Merge*" test
mvn "-Dtest=StudyTimingMappingUnitTest,StudySessionFlowIntegrationTest,StudySchedulerLegacyBehaviorIntegrationTest" test
mvn "-Dtest=FeatureTelemetryIntegrationTest,FeaturePerformanceValidationIT" test
```

## Local/Dev Seed Data

Enable deterministic seed users/decks/cards for local testing:

```powershell
cd backend
./scripts/run-local-with-seed.ps1
```

Or run manually:

```powershell
cd backend
$env:APP_SEED_LOCAL_DEV_ENABLED = "true"
$env:APP_SEED_LOCAL_DEV_PASSWORD = "khaleo"
$env:APP_SEED_LOCAL_DEV_RESET_ON_STARTUP = "true"
mvn spring-boot:run
```

Seeded login accounts (password default: `khaleo`):

- `admin@khaleo.app`
- `khaleo@khaleo.app`
- `learner+01@khaleo.app`
- `learner+02@khaleo.app`
- `learner+03@khaleo.app`
- `learner+04@khaleo.app`
- `learner+05@khaleo.app`
- `learner+blocked@khaleo.app` (banned)
- `learner+unverified@khaleo.app` (unverified)

Seed baseline:

- 8 users
- 12 decks (6 public + 6 private)
- 120 cards (10 cards/deck)

Authentication default behavior:

- Email verification is disabled by default (`APP_AUTH_EMAIL_VERIFICATION_REQUIRED=false`).
- Registration is direct email+password signup without verify-email blocking.

## Keep Local Database Between Restarts

Use Docker Compose local MySQL service; data is persisted in Docker volume `khaleo_mysql_data`:

```powershell
cd backend
./scripts/start-local-db.ps1
```

Do not remove the volume if you want to keep data. Avoid `docker compose down -v`.

## Shared Aurora-Only Dev Workflow (No Local Docker DB)

If your team wants one consistent shared database, run backend locally against Aurora through SSM tunnel.

Setup once:

```powershell
cd backend/scripts
Copy-Item sync-config.sample.ps1 sync-config.ps1
```

Edit `sync-config.ps1` values (AWS profile/region, DB secret, backend tag/instance).

Start Aurora tunnel:

```powershell
cd backend/scripts
./start-aurora-tunnel.ps1
```

Stop tunnel:

```powershell
./stop-aurora-tunnel.ps1
```

Run backend against shared Aurora:

```powershell
./run-backend-with-aurora.ps1
```

Seed shared Aurora baseline data (idempotent):

```powershell
./seed-shared-aurora.ps1
```

Important:

- Flyway migrations run on startup and keep schema aligned.
- Shared seed account defaults include `khaleo@khaleo.app / khaleo` and `admin@khaleo.app / khaleo`.
- Shared DB means all team writes affect everyone; use branch/staging database separation if needed.

## Aurora Sync Scripts (Local <-> Aurora)

Prerequisites:

- AWS CLI with SSM permission
- `mysql` and `mysqldump` installed in PATH
- A running backend EC2 target with SSM agent

Setup once:

```powershell
cd backend/scripts
Copy-Item sync-config.sample.ps1 sync-config.ps1
```

Edit `sync-config.ps1` values for your account/region/secret.

Sync local -> Aurora:

```powershell
cd backend/scripts
./sync-local-to-aurora.ps1
```

Sync Aurora -> local (drop/recreate local DB):

```powershell
cd backend/scripts
./sync-aurora-to-local.ps1
```

Non-interactive mode:

```powershell
./sync-local-to-aurora.ps1 -Force
./sync-aurora-to-local.ps1 -Force
```

## Feature 008 Verification Sequence

1. Run contract tests for public discovery/import and conflict endpoints.
2. Run integration tests for first import and re-import conflict/no-conflict paths.
3. Run study scheduling tests for new-card mapping and non-new fallback behavior.
4. Run telemetry/performance validation suite for cross-cutting signals.

## Notes

- `StudySessionController` endpoints require authentication and verified email.
- Import/re-import operations continue to enforce private ownership and source public visibility constraints.
- Performance suite validates scheduler decision-path speed targets used in SC-003 and SC-006 acceptance checks.
