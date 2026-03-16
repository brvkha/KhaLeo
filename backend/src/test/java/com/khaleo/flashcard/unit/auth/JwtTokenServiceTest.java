package com.khaleo.flashcard.unit.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.khaleo.flashcard.service.auth.JwtTokenService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;

class JwtTokenServiceTest {

    private static final String SECRET = "test-secret-test-secret-test-secret-123456";

    @Test
    void shouldCreateTokenWithClaimsAndSubject() {
        JwtTokenService service = new JwtTokenService(SECRET, "khaleo-test", 15);
        String token = service.createAccessToken("user-123", Map.of("role", "ROLE_USER"));

        Jws<Claims> parsed = service.parseAndValidate(token);
        assertThat(parsed.getPayload().getSubject()).isEqualTo("user-123");
        assertThat(parsed.getPayload().get("role", String.class)).isEqualTo("ROLE_USER");
    }

    @Test
    void shouldRejectExpiredToken() {
        JwtTokenService service = new JwtTokenService(SECRET, "khaleo-test", 15);
        String expiredToken = service.createToken("user-123", Instant.now().minusSeconds(10), Map.of());

        assertThatThrownBy(() -> service.parseAndValidate(expiredToken))
                .isInstanceOf(ExpiredJwtException.class);
    }
}