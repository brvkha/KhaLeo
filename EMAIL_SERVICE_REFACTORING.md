# Backend Email Service Refactoring - Local vs Production

## Problem
After `terraform destroy`, the registration endpoint returned 500 errors because AWS SES was unavailable, but the code had a hard dependency on it.

## Solution
Created an abstraction layer with two implementations:

### Architecture Changes

```
EmailService (Interface)
    ├── MockEmailService (Local Dev)
    └── SesEmailService (Production)
```

### New Files Created

1. **EmailService.java** - Interface defining email service contract
   - `sendVerificationEmail(toEmail, token)`
   - `sendPasswordResetEmail(toEmail, token)`

2. **MockEmailService.java** - Local development implementation
   - Logs emails to console instead of sending
   - Active by default in non-production environments
   - No AWS credentials needed

### Files Updated

1. **SesEmailService.java**
   - Now implements `EmailService` interface
   - Added `@Profile("production")` - only active in production
   - Added `@ConditionalOnProperty("app.email.provider", "ses")` - requires explicit configuration

2. **RegistrationService.java**
   - Changed dependency from `SesEmailService` → `EmailService`
   - Now uses injected interface instead of concrete class

3. **PasswordResetService.java**
   - Changed dependency from `SesEmailService` → `EmailService`
   - Now uses injected interface instead of concrete class

4. **application.yml**
   - Added `app.auth.email.provider` configuration
   - Default: `${APP_EMAIL_PROVIDER:mock}` (mock emails in local dev)
   - Can override to `app.email.provider=ses` for production

## How It Works

### Local Development (Default)
```bash
export APP_EMAIL_PROVIDER=mock  # Optional - this is the default
mvn spring-boot:run
```
- **MockEmailService** is activated
- Registration works - emails are logged to console
- No AWS credentials needed

Example console output:
```
🔷 [MOCK EMAIL] Verification email to: user@example.com with token: abc123...
🔷 [MOCK EMAIL] Verification URL: http://localhost:5173/verify?token=abc123...
```

### Production
```bash
export APP_EMAIL_PROVIDER=ses
export SPRING_PROFILES_ACTIVE=production
export AUTH_EMAIL_FROM=noreply@company.com
# ... AWS credentials configured
mvn spring-boot:run
```
- **SesEmailService** is activated
- Real emails sent via AWS SES
- Registration requires valid AWS SES configuration

## Benefits

✅ **Local Development**: Works out of the box without AWS  
✅ **Easy Testing**: See mock emails in logs instantly  
✅ **Production Ready**: Switch to real SES with one config change  
✅ **Clean Architecture**: Dependency inversion using interfaces  
✅ **Spring Best Practices**: Profile-based & property-based bean selection  
✅ **No Breaking Changes**: Existing code structure preserved  

## Testing

### Test New User Registration Locally
1. Start backend: `mvn spring-boot:run`
2. Frontend: `npm run dev`
3. Try to register new account
4. Check backend logs for mock email with verification token
5. Copy token and verify account locally

### Verify Spring Loaded Correct Bean
Check logs on startup:
- **Local Dev**: `MockEmailService` bean created
- **Production**: `SesEmailService` bean created
