# Backend

## Local Run

1. Reset local MySQL to default clean state:

```powershell
cd backend
./scripts/reset-local-database.ps1
```

2. Start the backend:

```powershell
cd backend
mvn spring-boot:run
```

You can also run `FlashcardBackendApplication` directly in IntelliJ.

## Local Seed Behavior

- Local seed is enabled by default (`app.seed.local-dev.enabled=true` in non-production).
- Local seed resets data on startup unless overridden.
- Flyway migrations always run before seed.

Optional environment overrides:

```powershell
$env:APP_SEED_LOCAL_DEV_ENABLED = "true"
$env:APP_SEED_LOCAL_DEV_RESET_ON_STARTUP = "true"
$env:APP_SEED_LOCAL_DEV_PASSWORD = "khaleo"
```

## Seeded Accounts

Default password: `khaleo`

- `admin@khaleo.app`
- `khaleo@khaleo.app`
- `learner+01@khaleo.app`
- `learner+02@khaleo.app`
- `learner+03@khaleo.app`
- `learner+04@khaleo.app`
- `learner+05@khaleo.app`
- `learner+blocked@khaleo.app`
- `learner+unverified@khaleo.app`

Seed data includes multiple subject decks and one large English deck with 1500 cards.

## Useful Commands

```bash
mvn test
mvn -DskipTests package
```
