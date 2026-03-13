<!--
Sync Impact Report
- Version change: template -> 1.0.0
- Modified principles:
	- Template Principle 1 -> I. Technology Standards And Architectural Boundaries
	- Template Principle 2 -> II. Security By Default
	- Template Principle 3 -> III. Performance, Scalability, And Observability
	- Template Principle 4 -> IV. Quality Gates And Testing Discipline
	- Template Principle 5 -> V. Learning Algorithm Fidelity And Product Rules
- Added sections:
	- Technology Standards
	- Compliance And Governance
- Removed sections: None
- Templates requiring updates:
	- ✅ updated .specify/templates/plan-template.md
	- ✅ updated .specify/templates/spec-template.md
	- ✅ updated .specify/templates/tasks-template.md
	- ✅ reviewed .specify/templates/commands/*.md (directory not present; no updates required)
- Follow-up TODOs: None
-->

# Kha Leo Flashcard Constitution

## Core Principles

### I. Technology Standards And Architectural Boundaries
Kha Leo Flashcard MUST be implemented as a replicated monolith. The frontend MUST use
React with Tailwind CSS. The backend MUST use Java 17, Spring Boot, Hibernate, and
Flyway. Core relational data MUST be stored in AWS Aurora MySQL, and learning activity
logs MUST be stored in AWS DynamoDB. Infrastructure provisioning and drift management
MUST be performed exclusively through Terraform. Production delivery MUST use S3 and
CloudFront for frontend assets, and three Dockerized backend instances on EC2 in private
subnets across three availability zones behind an Application Load Balancer, WAF, and
Route 53-managed DNS. Any deviation from this stack or topology requires an explicit
constitutional amendment because architectural consistency is the primary control for
operability and portfolio credibility.

### II. Security By Default
Authentication MUST be stateless and JWT-based with 15-minute access tokens and 7-day
refresh tokens. Email verification via AWS SES MUST be completed before a user can access
learning workflows. Authentication flows MUST enforce rate limiting such that five failed
login attempts trigger a one-day account lockout. User-uploaded media MUST be limited to
images and audio files up to 5 MB and MUST be uploaded directly to S3 by presigned URL;
backend services MUST not proxy media uploads. Secrets, signing keys, and infrastructure
credentials MUST never be hard-coded, logged, or committed to the repository. Security
controls are non-negotiable because the system handles personal learning data and public
internet traffic.

### III. Performance, Scalability, And Observability
The system MUST be designed for an expected load of 50 users with up to 30 concurrent
active users while preserving responsive study sessions and administrative workflows.
Every production service MUST emit structured JSON logs asynchronously to Splunk via HEC,
instrument application performance monitoring through the New Relic Java Agent, and
publish CloudWatch alarms for backend 5xx error conditions. APIs, background jobs, and
infrastructure changes MUST be designed so observability is available on day one rather
than added after incidents. Performance and telemetry are mandatory because the chosen
portfolio architecture is only credible if it can be operated and diagnosed under load.

### IV. Quality Gates And Testing Discipline
All externally exposed backend capabilities MUST be delivered as RESTful APIs, and every
list endpoint MUST implement pagination. The project MUST maintain at least 80% overall
test coverage with a testing pyramid target of 60% unit, 30% integration, and 10% end-
to-end coverage. Changes affecting persistence, authentication, the SM-2 scheduling flow,
or infrastructure integration MUST include automated tests at the appropriate layer before
release approval. Flyway migrations MUST be the sole mechanism for relational schema
evolution. Code reviews MUST reject work that lowers test discipline, bypasses pagination,
or introduces undocumented operational behavior.

### V. Learning Algorithm Fidelity And Product Rules
The application MUST faithfully implement Anki's SM-2 spaced repetition algorithm unless a
future constitutional amendment explicitly defines and justifies a compatible evolution.
Card lifecycle handling MUST preserve the states New, Learning, Mastered (waiting), and
Review as first-class domain concepts. Daily learning limits MUST be enforced at the
account level rather than only in the client. Any feature that changes scheduling,
progression, or card-state transitions MUST document how algorithmic correctness is
preserved and MUST include tests that prove those rules. Product value depends on trust in
the learning model, so algorithmic behavior is governed as a compliance concern, not a UX
preference.

## Technology Standards

- Frontend deliverables MUST remain compatible with a React and Tailwind CSS codebase and
	deploy as static assets through S3 and CloudFront.
- Backend deliverables MUST remain compatible with Java 17 and Spring Boot packaging as a
	Dockerized executable JAR on EC2.
- Aurora MySQL MUST hold transactional domain data such as users, decks, cards,
	scheduling metadata, and account-level limits.
- DynamoDB MUST hold learning activity and event-style logs that benefit from flexible,
	high-write access patterns.
- Terraform plans and modules MUST define all AWS infrastructure resources, including ALB,
	WAF, Route 53, VPC networking, subnets, EC2, S3, CloudFront, SES dependencies, and
	monitoring resources.
- Feature designs MUST explain relational and non-relational persistence choices when data
	crosses Aurora and DynamoDB boundaries.

## Compliance And Governance

- Specifications MUST identify any impact on SM-2 scheduling rules, card-state transitions,
	daily learning limits, JWT flows, email verification, media upload paths, and required
	observability.
- Implementation plans MUST include a constitution check covering stack compliance,
	security controls, observability, pagination, migration strategy, and test coverage.
- Task breakdowns MUST contain explicit work for testing, observability, security, and
	infrastructure changes whenever those concerns are touched.
- Pull requests MUST document constitutional impacts, including any changes to API
	contracts, database migrations, Terraform modules, authentication flows, or scheduling
	logic.
- Release approval MUST be blocked if required monitoring, alarms, tests, or security
	controls are absent.

## Governance

This constitution supersedes conflicting local conventions and planning shortcuts.
Amendments require a documented proposal, review by project maintainers, an explanation of
why existing rules are insufficient, and updates to any affected templates or delivery
artifacts in the same change. Constitutional versioning follows semantic versioning:
MAJOR for incompatible governance changes or principle removal, MINOR for new principles or
materially expanded mandates, and PATCH for clarifications that do not change enforcement.
Every feature specification, implementation plan, task list, pull request, and release
review MUST include an explicit compliance check against this constitution. Compliance
reviews MUST verify SM-2 fidelity, account-level learning-limit enforcement, security
requirements, observability, testing evidence, and infrastructure alignment before work is
approved for production.

**Version**: 1.0.0 | **Ratified**: 2026-03-13 | **Last Amended**: 2026-03-13
