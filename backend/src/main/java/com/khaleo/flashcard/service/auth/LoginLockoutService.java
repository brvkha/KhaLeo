package com.khaleo.flashcard.service.auth;

import com.khaleo.flashcard.entity.User;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class LoginLockoutService {

    private final int maxFailedAttempts;
    private final long lockoutDurationSeconds;

    public LoginLockoutService(
            @Value("${app.auth.lockout.max-failed-attempts:5}") int maxFailedAttempts,
            @Value("${app.auth.lockout.duration-seconds:86400}") long lockoutDurationSeconds) {
        this.maxFailedAttempts = maxFailedAttempts;
        this.lockoutDurationSeconds = lockoutDurationSeconds;
    }

    public void ensureNotLocked(User user) {
        if (isCurrentlyLocked(user)) {
            throw new AuthDomainException(
                    HttpStatus.LOCKED,
                    AuthErrorCode.ACCOUNT_LOCKED,
                    "Account is locked until " + user.getAccountLockedUntil());
        }
    }

    public void onFailedAttempt(User user) {
        int nextAttempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(nextAttempts);
        if (nextAttempts >= maxFailedAttempts) {
            user.setAccountLockedUntil(Instant.now().plusSeconds(lockoutDurationSeconds));
        }
    }

    public void onSuccessfulLogin(User user) {
        if (user.getFailedLoginAttempts() > 0 || user.getAccountLockedUntil() != null) {
            user.setFailedLoginAttempts(0);
            user.setAccountLockedUntil(null);
        }
    }

    public boolean isCurrentlyLocked(User user) {
        return user.getAccountLockedUntil() != null && user.getAccountLockedUntil().isAfter(Instant.now());
    }
}