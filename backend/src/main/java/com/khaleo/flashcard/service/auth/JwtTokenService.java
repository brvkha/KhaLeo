package com.khaleo.flashcard.service.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.time.Duration;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtTokenService {

    private final SecretKey signingKey;
    private final String issuer;
    private final Duration accessTokenTtl;

    public JwtTokenService(
            @Value("${app.auth.jwt.secret}") String secret,
            @Value("${app.auth.jwt.issuer}") String issuer,
            @Value("${app.auth.jwt.access-token-ttl-minutes:15}") long accessTokenTtlMinutes) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.issuer = issuer;
        this.accessTokenTtl = Duration.ofMinutes(accessTokenTtlMinutes);
    }

    public String createToken(String subject, Instant expiresAt, Map<String, ?> claims) {
        return Jwts.builder()
                .issuer(issuer)
                .subject(subject)
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(expiresAt))
                .claims(claims)
                .signWith(signingKey)
                .compact();
    }

    public Jws<Claims> parseAndValidate(String token) {
        return Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token);
    }

    public String createAccessToken(String subject, Map<String, ?> claims) {
        return createToken(subject, Instant.now().plus(accessTokenTtl), claims);
    }

    public long accessTokenTtlSeconds() {
        return accessTokenTtl.getSeconds();
    }
}
