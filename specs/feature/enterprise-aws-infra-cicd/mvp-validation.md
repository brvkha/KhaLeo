# MVP Validation Report (US1)

Date: 2026-03-17
Feature: enterprise-aws-infra-cicd

## Scope Covered

- Phase 1 (T001-T004): Complete
- Phase 2 (T005-T010): Complete
- Phase 3 US1 (T011-T018): Complete

## Automated Validation Results

### Backend US1 contract test
- Test class: `BackendDeploymentWorkflowIT`
- Result: 2 passed, 0 failed
- Verified:
  - immutable artifact key path uses commit SHA (`backend/${{ steps.sha.outputs.value }}/app.jar`)
  - immutable rollout contract exists (Packer build + ASG instance refresh)
  - rollback guidance is emitted

### Frontend US1 contract test
- Test file: `frontend/tests/e2e/deploy-frontend-contract.spec.ts`
- Result: 1 passed, 0 failed
- Verified:
  - deploy workflow resolves deployment SHA
  - checkout uses resolved SHA
  - S3 sync + CloudFront invalidation commands exist
  - summary prints SHA and invalidation ID

## Workflow Contract Evidence

### Manual gated deploy flow
- `deploy-backend.yml`:
  - Trigger path includes `workflow_dispatch`
  - Branch gate requires `github.ref_name == 'main'`
  - Job uses `environment: production`
- `deploy-frontend.yml`:
  - Trigger path includes `workflow_dispatch`
  - Branch gate requires `github.ref_name == 'main'`
  - Job uses `environment: production`
- `ci.yml`:
  - `workflow-smoke-contract` job enforces deploy workflow gating semantics
  - Summary and fail gate require smoke-contract success

### Immutable SHA traceability
- Backend artifact upload path: `backend/${{ steps.sha.outputs.value }}/app.jar`
- Frontend deploy summary includes resolved artifact SHA
- Both deploy workflows accept manual `workflow_dispatch` input `artifactSha`

### Manual rollback path
- Backend summary and failure sections include rollback commands using `artifactSha=<known-good-sha>`
- Frontend summary and failure sections include rollback commands using `artifactSha=<known-good-sha>`
- Quickstart documents rollback sequence with matching backend/frontend SHA

## Operational Note

Live GitHub approval and end-to-end cloud deployment execution cannot be performed from this local environment. The implementation and tests validate the workflow contracts and deployment semantics required for MVP release readiness.
