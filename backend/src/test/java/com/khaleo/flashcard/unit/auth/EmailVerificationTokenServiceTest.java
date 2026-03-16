package com.khaleo.flashcard.unit.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.khaleo.flashcard.entity.EmailVerificationToken;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class EmailVerificationTokenServiceTest {

    @Test
    void shouldReportTokenNotExpiredWhenExpiryIsInFuture() {
        EmailVerificationToken token = EmailVerificationToken.builder()
                .token("token")
                .expiresAt(Instant.now().plusSeconds(60))
                .build();

        assertThat(token.isExpiredAt(Instant.now())).isFalse();
    }

    @Test
    void shouldReportTokenConsumedWhenConsumedAtExists() {
        EmailVerificationToken token = EmailVerificationToken.builder()
                .token("token")
                .expiresAt(Instant.now().plusSeconds(60))
                .consumedAt(Instant.now())
                .build();

        assertThat(token.isConsumed()).isTrue();
    }
}
