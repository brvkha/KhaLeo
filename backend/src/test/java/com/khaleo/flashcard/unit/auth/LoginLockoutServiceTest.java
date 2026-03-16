package com.khaleo.flashcard.unit.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.khaleo.flashcard.entity.User;
import com.khaleo.flashcard.service.auth.AuthDomainException;
import com.khaleo.flashcard.service.auth.AuthErrorCode;
import com.khaleo.flashcard.service.auth.LoginLockoutService;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class LoginLockoutServiceTest {

    @Test
    void shouldLockAfterFifthFailure() {
        LoginLockoutService service = new LoginLockoutService(5, 86400);
        User user = User.builder().failedLoginAttempts(0).build();

        for (int i = 0; i < 4; i++) {
            service.onFailedAttempt(user);
        }
        assertThat(user.getAccountLockedUntil()).isNull();

        service.onFailedAttempt(user);
        assertThat(user.getFailedLoginAttempts()).isEqualTo(5);
        assertThat(user.getAccountLockedUntil()).isAfter(Instant.now());
    }

    @Test
    void shouldResetFailuresOnSuccessfulLogin() {
        LoginLockoutService service = new LoginLockoutService(5, 86400);
        User user = User.builder()
                .failedLoginAttempts(3)
                .accountLockedUntil(Instant.now().plusSeconds(30))
                .build();

        service.onSuccessfulLogin(user);

        assertThat(user.getFailedLoginAttempts()).isZero();
        assertThat(user.getAccountLockedUntil()).isNull();
    }

    @Test
    void shouldThrowWhenAccountIsCurrentlyLocked() {
        LoginLockoutService service = new LoginLockoutService(5, 86400);
        User user = User.builder()
                .failedLoginAttempts(5)
                .accountLockedUntil(Instant.now().plusSeconds(60))
                .build();

        assertThatThrownBy(() -> service.ensureNotLocked(user))
                .isInstanceOf(AuthDomainException.class)
                .satisfies(ex -> assertThat(((AuthDomainException) ex).getErrorCode())
                        .isEqualTo(AuthErrorCode.ACCOUNT_LOCKED));
    }
}