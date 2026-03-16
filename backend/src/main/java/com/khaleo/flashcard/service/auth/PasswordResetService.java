package com.khaleo.flashcard.service.auth;

import com.khaleo.flashcard.entity.PasswordResetToken;
import com.khaleo.flashcard.entity.RefreshToken;
import com.khaleo.flashcard.entity.User;
import com.khaleo.flashcard.repository.PasswordResetTokenRepository;
import com.khaleo.flashcard.repository.RefreshTokenRepository;
import com.khaleo.flashcard.repository.UserRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
@SuppressWarnings("null")
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final SesEmailService sesEmailService;
    private final AuthAuditLogger authAuditLogger;

    public void forgotPassword(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required.");
        }

        String normalizedEmail = normalizeEmail(email);
        userRepository.findByEmail(normalizedEmail).ifPresent(this::issueResetTokenAndEmail);
        authAuditLogger.logEvent("auth_password_reset_requested", Map.of("email", normalizedEmail));
    }

    public void resetPassword(String tokenValue, String newPassword) {
        if (tokenValue == null || tokenValue.isBlank() || newPassword == null || newPassword.isBlank()) {
            throw new IllegalArgumentException("Token and new password are required.");
        }

        PasswordResetToken token = passwordResetTokenRepository.findByToken(tokenValue)
                .orElseThrow(() -> new AuthDomainException(
                        HttpStatus.NOT_FOUND,
                        AuthErrorCode.INVALID_RESET_TOKEN,
                        "Reset token was not found."));

        if (token.isConsumed()) {
            throw new AuthDomainException(HttpStatus.NOT_FOUND, AuthErrorCode.INVALID_RESET_TOKEN, "Reset token is invalid.");
        }
        if (token.isExpiredAt(Instant.now())) {
            throw new AuthDomainException(HttpStatus.GONE, AuthErrorCode.EXPIRED_RESET_TOKEN, "Reset token has expired.");
        }

        User user = token.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setFailedLoginAttempts(0);
        user.setAccountLockedUntil(null);
        userRepository.save(user);

        token.setConsumedAt(Instant.now());
        passwordResetTokenRepository.save(token);

        List<RefreshToken> activeTokens =
                refreshTokenRepository.findByUserIdAndRevokedAtIsNullAndExpiresAtAfter(user.getId(), Instant.now());
        Instant revokedAt = Instant.now();
        activeTokens.forEach(rt -> rt.setRevokedAt(revokedAt));
        refreshTokenRepository.saveAll(activeTokens);

        authAuditLogger.logEvent("auth_password_reset_completed", Map.of("userId", user.getId()));
    }

    private void issueResetTokenAndEmail(User user) {
        String resetTokenValue = UUID.randomUUID().toString();
        PasswordResetToken token = PasswordResetToken.builder()
                .token(resetTokenValue)
                .user(user)
                .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                .build();
        passwordResetTokenRepository.save(token);
        sesEmailService.sendPasswordResetEmail(user.getEmail(), resetTokenValue);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}