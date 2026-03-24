# Local Development Setup

## After Terraform Destroy (Running Locally Without AWS)

This guide explains how to run KhaLeo locally after destroying your AWS infrastructure with Terraform.

## Key Issue: Email Verification

The registration endpoint sends verification emails via AWS SES. Without AWS credentials (after `terraform destroy`), the registration will fail with a **500 error**.

**Solution**: Use pre-verified seeded users for local development instead of registering new users.

## Setup Steps

### 1. Configure Backend for Local Development (UPDATED)

**Now with MockEmailService - Email sending is mocked by default!**

Set these environment variables before starting the backend:

```bash
# Enable local dev data seeding (OPTIONAL)
APP_SEED_LOCAL_DEV_ENABLED=true

# Set default password for seeded users (optional, defaults to "khaleo")
APP_SEED_LOCAL_DEV_PASSWORD=khaleo

# Email provider - use "mock" for local dev (default), "ses" for production
APP_EMAIL_PROVIDER=mock
```

**Linux/Mac:**
```bash
export APP_SEED_LOCAL_DEV_ENABLED=true
export APP_SEED_LOCAL_DEV_PASSWORD=khaleo
export APP_EMAIL_PROVIDER=mock
```

**Windows PowerShell:**
```powershell
$env:APP_SEED_LOCAL_DEV_ENABLED = "true"
$env:APP_SEED_LOCAL_DEV_PASSWORD = "khaleo"
$env:APP_EMAIL_PROVIDER = "mock"
```

> **Note**: `APP_EMAIL_PROVIDER=mock` is the **default** - emails won't actually send, they'll just log to the console. You can now register new users and they'll receive mock verification emails logged to the terminal!

### 2. Start the Backend

```bash
cd backend
mvn spring-boot:run
```

On startup, the application will automatically seed your database with test users and sample decks.

### 3. Use Seeded Users for Login

After startup, you can log in with these pre-verified credentials:

| Email | Password | Role | Purpose |
|-------|----------|------|---------|
| `admin@khaleo.app` | `khaleo` | ADMIN | Administrator access |
| `khaleo@khaleo.app` | `khaleo` | USER | Regular user |
| `learner+01@khaleo.app` | `khaleo` | USER | Regular learner 1 |
| `learner+02@khaleo.app` | `khaleo` | USER | Regular learner 2 |
| `learner+03@khaleo.app` | `khaleo` | USER | Regular learner 3 |
| `learner+04@khaleo.app` | `khaleo` | USER | Regular learner 4 |
| `learner+05@khaleo.app` | `khaleo` | USER | Regular learner 5 |

### 4. Start the Frontend

```bash
cd frontend
npm install
npm run dev
```

Access the app at `http://localhost:5173` (or the port shown in the terminal).

## Why You Can't Register Locally

~~After `terraform destroy`, AWS resources are deleted:~~
- **FIXED!** Now uses `MockEmailService` by default for local development
- Email sending is simulated and logged to console
- No AWS SES credentials needed locally

Each seeded user is pre-verified, so they don't require email confirmation.

### Two Email Service Implementations

The application now supports two email service implementations:

#### 1. **MockEmailService** (Local Development - Default)
- **When Active**: Spring profile != "production" AND `APP_EMAIL_PROVIDER=mock` (default)
- **Behavior**: Logs emails to console instead of sending via AWS SES
- **Use Case**: Local development, no AWS credentials needed
- **Example Log Output**:
```
🔷 [MOCK EMAIL] Verification email to: user@example.com with token: 0c4a9d8e-...
🔷 [MOCK EMAIL] Verification URL: http://localhost:5173/verify?token=0c4a9d8e-...
```

#### 2. **SesEmailService** (Production)
- **When Active**: Spring profile = "production" OR `APP_EMAIL_PROVIDER=ses`
- **Behavior**: Sends real emails via AWS SES
- **Use Case**: Production deployment, requires AWS credentials
- **Configuration**: `APP_EMAIL_PROVIDER=ses`

### Usage Examples

**Local Development** (make new user registrations):
```bash
# No env vars needed - defaults to mock email service
cd backend
mvn spring-boot:run
```
Then register a new account - verification email will be logged to console.

**With Seeded Users** (faster testing):
```bash
export APP_SEED_LOCAL_DEV_ENABLED=true
cd backend
mvn spring-boot:run
```
Then log in with pre-created users.

**Production** (real AWS SES):
```bash
export APP_EMAIL_PROVIDER=ses
export SPRING_PROFILES_ACTIVE=production
export AUTH_EMAIL_FROM=your-verified-ses-email@example.com
# ... other AWS credentials
mvn spring-boot:run
```

## To Create Additional Test Users Manually

Modify `/backend/src/main/java/com/khaleo/flashcard/config/seed/LocalDevDataSeeder.java` to add more users in the `seedUsers()` method. Users created via the seeder are automatically marked as email-verified.

## Re-enabling AWS (When Ready)

When you rebuild your AWS infrastructure:

1. Re-run Terraform to create AWS SES and other resources
2. Configure AWS credentials on your local machine or in the application
3. Users will be able to register normally and receive verification emails
4. Disable `APP_SEED_LOCAL_DEV_ENABLED` to avoid re-seeding data

## Database

Local development uses the database from Docker:
```bash
cd backend/scripts
./start-local-db.sh    # Start PostgreSQL in Docker
./stop-aurora-tunnel.sh # Stop if needed
```

Or use your local PostgreSQL instance if configured in `application.yml`.

## Troubleshooting

**Q: Getting "Internal Server Error" on registration?**
- A: Make sure `APP_SEED_LOCAL_DEV_ENABLED=true` is set (for bypass) OR use seeded users instead
- The environment variable must be set BEFORE starting the backend

**Q: Seeded users aren't appearing in the database?**
- A: Check the backend logs for "local-dev seed" messages
- Ensure the Spring profile is not "production"
- Clear the database and restart the backend

**Q: What password should I use?**
- A: By default `khaleo`. Change with `APP_SEED_LOCAL_DEV_PASSWORD` env var
