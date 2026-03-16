# KhaLeo Development Guidelines

Auto-generated from all feature plans. Last updated: 2026-03-13

## Active Technologies
- Java 17 (Spring Boot backend), SQL (MySQL 8.0 via Flyway), Terraform HCL + Spring Security 6.x, Spring Boot Web/Validation, (002-auth-security-verification)
- Aurora MySQL for users and auth-token state; DynamoDB unchanged for study activity logs (002-auth-security-verification)
- Java 17 (Spring Boot 3.3.x), SQL (MySQL 8 via Flyway), Terraform HCL + Spring Web, Spring Security, Spring Data JPA/Hibernate, Flyway, AWS SDK v2 (`s3`, `s3-presigner`), JUnit 5, Testcontainers (003-deck-card-s3-media)
- Aurora MySQL for decks/cards/media references and authorization records; DynamoDB unchanged for study activity logs (003-deck-card-s3-media)
- Java 17 (Spring Boot 3.3.x), SQL (Aurora MySQL 8 via Flyway), Terraform HCL + Spring Web, Spring Security, Spring Data JPA/Hibernate, Flyway, AWS SDK v2 DynamoDB Enhanced Client, Spring Async, JUnit 5, Testcontainers (004-sm2-study-logging)
- Aurora MySQL for card-learning state and quota calculations; DynamoDB (`StudyActivityLog`) for append-only study activity entries (004-sm2-study-logging)
- Java 17 (Spring Boot 3.3.2), SQL (Aurora MySQL via Flyway), Terraform HCL, GitHub Actions YAML + Spring Web, Spring Security, Spring Data JPA/Hibernate, Flyway, AWS SDK v2 (S3/SES/DynamoDB), Splunk Java Logging Library, JUnit 5/Testcontainers (005-admin-observability-devops)
- Aurora MySQL for user/deck/card/admin state, DynamoDB for study activity logs, S3 for immutable backend artifacts (005-admin-observability-devops)

- Java 17 (backend), SQL (MySQL 8.0 dialect), Terraform HCL + Spring Boot, Spring Data JPA, Hibernate, Flyway, (001-core-database-entities)

## Project Structure

```text
backend/
frontend/
tests/
```

## Commands

# Add commands for Java 17 (backend), SQL (MySQL 8.0 dialect), Terraform HCL

## Code Style

Java 17 (backend), SQL (MySQL 8.0 dialect), Terraform HCL: Follow standard conventions

## Recent Changes
- 005-admin-observability-devops: Added Java 17 (Spring Boot 3.3.2), SQL (Aurora MySQL via Flyway), Terraform HCL, GitHub Actions YAML + Spring Web, Spring Security, Spring Data JPA/Hibernate, Flyway, AWS SDK v2 (S3/SES/DynamoDB), Splunk Java Logging Library, JUnit 5/Testcontainers
- 004-sm2-study-logging: Added Java 17 (Spring Boot 3.3.x), SQL (Aurora MySQL 8 via Flyway), Terraform HCL + Spring Web, Spring Security, Spring Data JPA/Hibernate, Flyway, AWS SDK v2 DynamoDB Enhanced Client, Spring Async, JUnit 5, Testcontainers
- 003-deck-card-s3-media: Added Java 17 (Spring Boot 3.3.x), SQL (MySQL 8 via Flyway), Terraform HCL + Spring Web, Spring Security, Spring Data JPA/Hibernate, Flyway, AWS SDK v2 (`s3`, `s3-presigner`), JUnit 5, Testcontainers


<!-- MANUAL ADDITIONS START -->
<!-- MANUAL ADDITIONS END -->
