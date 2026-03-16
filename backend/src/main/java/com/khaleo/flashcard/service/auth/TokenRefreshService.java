package com.khaleo.flashcard.service.auth;

import com.khaleo.flashcard.entity.RefreshToken;
import com.khaleo.flashcard.entity.User;
import com.khaleo.flashcard.repository.RefreshTokenRepository;
import java.time.Instant;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class TokenRefreshService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenService jwtTokenService;
    private final AuthAuditLogger authAuditLogger;

    public RefreshResult refreshAccessToken(String refreshTokenValue) {
        if (refreshTokenValue == null || refreshTokenValue.isBlank()) {
            throw new AuthDomainException(HttpStatus.UNAUTHORIZED, AuthErrorCode.INVALID_REFRESH_TOKEN, "Invalid refresh token.");
        }

        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenValue)
                .orElseThrow(() -> new AuthDomainException(
                        HttpStatus.UNAUTHORIZED,
                        AuthErrorCode.INVALID_REFRESH_TOKEN,
                        "Invalid refresh token."));

        if (refreshToken.getRevokedAt() != null) {
            throw new AuthDomainException(HttpStatus.UNAUTHORIZED, AuthErrorCode.INVALID_REFRESH_TOKEN, "Refresh token revoked.");
        }

        if (!refreshToken.getExpiresAt().isAfter(Instant.now())) {
            throw new AuthDomainException(HttpStatus.UNAUTHORIZED, AuthErrorCode.EXPIRED_REFRESH_TOKEN, "Refresh token expired.");
        }

        User user = refreshToken.getUser();
        if (user.getBannedAt() != null) {
            authAuditLogger.logEvent("auth_refresh_blocked_banned", Map.of("userId", user.getId()));
            throw new AuthDomainException(HttpStatus.FORBIDDEN, AuthErrorCode.BANNED_USER_REQUEST_DENIED, "Banned account access denied.");
        }

        String accessToken = jwtTokenService.createAccessToken(user.getId().toString(), Map.of("role", user.getRole().name()));
        authAuditLogger.logEvent("auth_refresh_success", Map.of("userId", user.getId()));
        return new RefreshResult(accessToken, jwtTokenService.accessTokenTtlSeconds());
    }

    public record RefreshResult(String accessToken, long expiresIn) {
    }
}