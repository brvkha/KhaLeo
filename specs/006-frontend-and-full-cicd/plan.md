# Implementation Plan: Frontend Application and Full Push-to-Deploy CI/CD

**Branch**: `006-frontend-and-full-cicd` | **Date**: 2026-03-16 | **Spec**: `spec.md`

## Summary

Deliver the missing frontend application and production-grade CI/CD automation:

- Frontend: React + Tailwind SPA with auth, deck/card CRUD, study flow, and admin dashboard.
- CI/CD: push-to-main pipeline that tests/builds/deploys backend and frontend with immutable artifacts and failure visibility.

## Technical Context

**Language/Version**: TypeScript + React 18 (frontend), Java 17 + Spring Boot (backend), GitHub Actions YAML  
**Primary Dependencies**: Vite, React Router, TanStack Query, Tailwind CSS, Playwright/Cypress, JUnit/Testcontainers  
**Storage**: Aurora MySQL, DynamoDB, S3 (media + frontend hosting + backend artifacts)  
**Testing**: Vitest + React Testing Library + E2E runner for frontend; Maven test suites for backend  
**Target Platform**: AWS S3 + CloudFront (frontend), EC2 private subnets with SSM deployment (backend)

## Architecture Decisions

1. Frontend will be a separate top-level `frontend/` workspace built with Vite + TypeScript.
2. Auth strategy will use access token + refresh flow with protected routes and silent refresh.
3. API integration uses typed service layer and centralized HTTP client error mapping.
4. CI/CD split into reusable workflows:
   - `ci.yml` for test/build gates on PR/push.
   - `deploy-backend.yml` for backend rollout.
   - `deploy-frontend.yml` for static site rollout.
5. Deployment orchestration uses commit SHA for artifact immutability and rollback consistency.

## Environments

- `dev`: optional continuous deployment from `develop`.
- `production`: required deployment from `main` with environment protection approval (optional but recommended).

## Risks & Mitigations

- Secret sprawl risk: centralize required secrets in GitHub Environments, document once in quickstart.
- Partial deployment failure: enforce fail-on-any-target while still dispatching all targets.
- Frontend/backend contract drift: add contract checks in CI and typed DTO mapping tests.

## Completion Definition

- Frontend routes and critical flows implemented and validated.
- CI gates pass on PR and push.
- Push to `main` automatically deploys frontend and backend.
- Rollback by commit SHA is documented and executable.
