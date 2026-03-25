# KhaLeo

KhaLeo is a flashcard learning platform with:

- Spring Boot backend (`backend`)
- React + Vite frontend (`frontend`)

## Quick Start (Local)

1. Reset local database to clean default state:

```powershell
cd backend
./scripts/reset-local-database.ps1
```

2. Run backend (choose one):

```powershell
cd backend
mvn spring-boot:run
```

or run `FlashcardBackendApplication` from IntelliJ.

3. Run frontend:

```powershell
cd frontend
npm.cmd install
npm.cmd run dev
```

## Seeded Local Data

Local seed is enabled by default in non-production and resets on startup.

Default login password: `khaleo`

- `admin@khaleo.app`
- `khaleo@khaleo.app`
- `learner+01@khaleo.app`
- `learner+02@khaleo.app`
- `learner+03@khaleo.app`
- `learner+04@khaleo.app`
- `learner+05@khaleo.app`
- `learner+blocked@khaleo.app`
- `learner+unverified@khaleo.app`

Seed includes diverse study decks and a large English deck (`EN-VOC-CORE-1500`) with 1500 cards.

## Project Layout

- `backend`: API, scheduler, persistence, local seed
- `frontend`: web app UI
- `infra`: deployment IaC and packer/terraform assets
