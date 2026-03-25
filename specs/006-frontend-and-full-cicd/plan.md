# Implementation Plan: Frontend Application and Production-Gated CI/CD

**Branch**: `006-frontend-and-full-cicd` | **Date**: 2026-03-16 | **Spec**: `spec.md`

## Summary

Deliver the missing frontend application and production-grade CI/CD automation:

- Frontend: React + Tailwind SPA with auth, deck/card CRUD, study flow, and admin dashboard.
- CI/CD: gated workflows that test/build and then deploy backend/frontend with immutable artifacts and explicit production approval.

## Technical Context

**Language/Version**: TypeScript + React 18 (frontend), Java 17 + Spring Boot (backend), GitHub Actions YAML  
**Primary Dependencies**: Vite, React Router, TanStack Query, Tailwind CSS, Playwright/Cypress, JUnit/Testcontainers  
**Storage**: MySQL-compatible relational store, DynamoDB, S3 (media + frontend hosting + backend artifacts)  
**Testing**: Vitest + React Testing Library + E2E runner for frontend; Maven test suites for backend  
**Target Platform**: AWS S3 + CloudFront (frontend), EC2 private subnets with immutable backend rollout (Packer AMI + ASG instance refresh)

## Architecture Decisions

1. Frontend will be a separate top-level `frontend/` workspace built with Vite + TypeScript.
2. Auth strategy will use access token + refresh flow with protected routes and silent refresh.
3. API integration uses typed service layer and centralized HTTP client error mapping.
4. CI/CD split into reusable workflows:
   - `ci.yml` for quality gates.
   - `deploy-backend.yml` for backend rollout.
   - `deploy-frontend.yml` for static site rollout.
5. Deployment orchestration uses commit SHA for artifact immutability and rollback consistency.

## Environments

- `production`: deployment via workflow dispatch with environment protection approval.

## Risks & Mitigations

- Secret sprawl risk: centralize required secrets in GitHub Environments, document once in quickstart.
- Partial deployment failure: enforce fail-on-any-target while still dispatching all targets.
- Frontend/backend contract drift: add contract checks in CI and typed DTO mapping tests.

## Completion Definition

- Frontend routes and critical flows implemented and validated.
- CI gates pass when workflow runs.
- Deploy workflows are runnable on demand with approval and immutable SHA targeting.
- Rollback by commit SHA is documented and executable.
