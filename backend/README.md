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

## Keep Local Database Between Restarts

Use Docker Compose local MySQL service; data is persisted in Docker volume `khaleo_mysql_data`:

```powershell
cd backend
./scripts/start-local-db.ps1
```

Do not remove the volume if you want to keep data. Avoid `docker compose down -v`.

## Feature 008 Verification Sequence

1. Run contract tests for public discovery/import and conflict endpoints.
2. Run integration tests for first import and re-import conflict/no-conflict paths.
3. Run study scheduling tests for new-card mapping and non-new fallback behavior.
4. Run telemetry/performance validation suite for cross-cutting signals.

## Notes

- `StudySessionController` endpoints require authentication and verified email.
- Import/re-import operations continue to enforce private ownership and source public visibility constraints.
- Performance suite validates scheduler decision-path speed targets used in SC-003 and SC-006 acceptance checks.
