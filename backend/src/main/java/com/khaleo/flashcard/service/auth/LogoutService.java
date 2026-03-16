package com.khaleo.flashcard.service.auth;

import com.khaleo.flashcard.entity.RefreshToken;
import com.khaleo.flashcard.repository.RefreshTokenRepository;
import java.time.Instant;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class LogoutService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final AuthAuditLogger authAuditLogger;

    public void logoutByRefreshToken(String refreshTokenValue) {
        if (refreshTokenValue == null || refreshTokenValue.isBlank()) {
            throw new IllegalArgumentException("Refresh token is required.");
        }

        refreshTokenRepository.findByToken(refreshTokenValue).ifPresent(this::revokeIfActive);
        authAuditLogger.logEvent("auth_logout_completed", Map.of("refreshTokenProvided", true));
    }

    private void revokeIfActive(RefreshToken token) {
        if (token.getRevokedAt() == null) {
            token.setRevokedAt(Instant.now());
            refreshTokenRepository.save(token);
        }
    }
}