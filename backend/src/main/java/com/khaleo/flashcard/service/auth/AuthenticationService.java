package com.khaleo.flashcard.service.auth;

import com.khaleo.flashcard.entity.User;
import com.khaleo.flashcard.entity.RefreshToken;
import com.khaleo.flashcard.repository.RefreshTokenRepository;
import com.khaleo.flashcard.repository.UserRepository;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
@SuppressWarnings("null")
public class AuthenticationService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthAuditLogger authAuditLogger;
    private final LoginLockoutService loginLockoutService;
    private final JwtTokenService jwtTokenService;

    @Value("${app.auth.jwt.refresh-token-ttl-days:7}")
    private long refreshTokenTtlDays;

    public LoginResult login(String email, String rawPassword) {
        User user = authenticateVerifiedUser(email, rawPassword);

        String refreshTokenValue = UUID.randomUUID().toString();
        RefreshToken refreshToken = RefreshToken.builder()
                .token(refreshTokenValue)
                .user(user)
                .expiresAt(Instant.now().plusSeconds(refreshTokenTtlDays * 24 * 60 * 60))
                .build();
        refreshTokenRepository.save(refreshToken);

        String accessToken = jwtTokenService.createAccessToken(
                user.getId().toString(),
                Map.of("role", user.getRole().name()));

        authAuditLogger.logEvent("auth_login_success", Map.of("userId", user.getId(), "email", user.getEmail()));
        return new LoginResult(accessToken, refreshTokenValue, jwtTokenService.accessTokenTtlSeconds());
    }

    public User authenticateVerifiedUser(String email, String rawPassword) {
        String normalizedEmail = normalizeEmail(email);
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> invalidCredentials(normalizedEmail));

        if (user.getBannedAt() != null) {
            authAuditLogger.logEvent("auth_login_blocked_banned", Map.of("email", normalizedEmail));
            throw new AuthDomainException(HttpStatus.FORBIDDEN, AuthErrorCode.BANNED_USER_REQUEST_DENIED, "Banned account access denied.");
        }

        if (loginLockoutService.isCurrentlyLocked(user)) {
            authAuditLogger.logEvent("auth_login_blocked_locked", Map.of("email", normalizedEmail));
            loginLockoutService.ensureNotLocked(user);
        }

        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            loginLockoutService.onFailedAttempt(user);
            if (loginLockoutService.isCurrentlyLocked(user)) {
                authAuditLogger.logEvent("auth_account_locked", Map.of("email", normalizedEmail));
            }
            userRepository.save(user);
            throw invalidCredentials(normalizedEmail);
        }

        if (!Boolean.TRUE.equals(user.getIsEmailVerified())) {
            authAuditLogger.logEvent("auth_login_blocked_unverified", Map.of("email", normalizedEmail));
            throw new AuthDomainException(
                    HttpStatus.FORBIDDEN,
                    AuthErrorCode.UNVERIFIED_EMAIL,
                    "Email verification is required before login.");
        }

        if (user.getFailedLoginAttempts() > 0 || user.getAccountLockedUntil() != null) {
            loginLockoutService.onSuccessfulLogin(user);
            userRepository.save(user);
        }

        authAuditLogger.logEvent("auth_login_verified", Map.of("userId", user.getId(), "email", normalizedEmail));
        return user;
    }

    private AuthDomainException invalidCredentials(String normalizedEmail) {
        authAuditLogger.logEvent("auth_login_failed", Map.of("email", normalizedEmail));
        return new AuthDomainException(HttpStatus.UNAUTHORIZED, AuthErrorCode.INVALID_CREDENTIALS, "Invalid credentials.");
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    public record LoginResult(String accessToken, String refreshToken, long expiresIn) {
    }
}
