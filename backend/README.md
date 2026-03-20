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

## Feature 008 Verification Sequence

1. Run contract tests for public discovery/import and conflict endpoints.
2. Run integration tests for first import and re-import conflict/no-conflict paths.
3. Run study scheduling tests for new-card mapping and non-new fallback behavior.
4. Run telemetry/performance validation suite for cross-cutting signals.

## Notes

- `StudySessionController` endpoints require authentication and verified email.
- Import/re-import operations continue to enforce private ownership and source public visibility constraints.
- Performance suite validates scheduler decision-path speed targets used in SC-003 and SC-006 acceptance checks.
