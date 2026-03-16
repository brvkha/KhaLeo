package com.khaleo.flashcard.integration.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;

import com.khaleo.flashcard.entity.EmailVerificationToken;
import com.khaleo.flashcard.entity.RefreshToken;
import com.khaleo.flashcard.entity.User;
import com.khaleo.flashcard.integration.support.IntegrationPersistenceTestBase;
import com.khaleo.flashcard.repository.EmailVerificationTokenRepository;
import com.khaleo.flashcard.repository.RefreshTokenRepository;
import com.khaleo.flashcard.repository.UserRepository;
import com.khaleo.flashcard.service.auth.AuthDomainException;
import com.khaleo.flashcard.service.auth.AuthErrorCode;
import com.khaleo.flashcard.service.auth.RegistrationService;
import com.khaleo.flashcard.service.auth.TokenRefreshService;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@SuppressWarnings("null")
class RefreshTokenRejectionIT extends IntegrationPersistenceTestBase {

    @Autowired
    private RegistrationService registrationService;

    @Autowired
    private EmailVerificationTokenRepository emailVerificationTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private TokenRefreshService tokenRefreshService;

    @MockBean
    private com.khaleo.flashcard.service.auth.SesEmailService sesEmailService;

    @BeforeEach
    void setUp() {
        doNothing().when(sesEmailService).sendVerificationEmail(anyString(), anyString());
        doNothing().when(sesEmailService).sendPasswordResetEmail(anyString(), anyString());
    }

    @Test
    void shouldRejectExpiredRefreshToken() {
        User user = createVerifiedUser("expired-refresh@example.com");
        String tokenValue = UUID.randomUUID().toString();
        refreshTokenRepository.save(RefreshToken.builder()
                .token(tokenValue)
                .user(user)
                                .expiresAt(Instant.now().plusSeconds(1))
                .build());

                try {
                        Thread.sleep(1200);
                } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new AssertionError("Interrupted while waiting for token expiry", e);
                }

        assertThatThrownBy(() -> tokenRefreshService.refreshAccessToken(tokenValue))
                .isInstanceOf(AuthDomainException.class)
                .satisfies(ex -> assertThat(((AuthDomainException) ex).getErrorCode())
                        .isEqualTo(AuthErrorCode.EXPIRED_REFRESH_TOKEN));
    }

    @Test
    void shouldRejectRevokedRefreshToken() {
        User user = createVerifiedUser("revoked-refresh@example.com");
        String tokenValue = UUID.randomUUID().toString();
        refreshTokenRepository.save(RefreshToken.builder()
                .token(tokenValue)
                .user(user)
                .expiresAt(Instant.now().plusSeconds(300))
                .revokedAt(Instant.now())
                .build());

        assertThatThrownBy(() -> tokenRefreshService.refreshAccessToken(tokenValue))
                .isInstanceOf(AuthDomainException.class)
                .satisfies(ex -> assertThat(((AuthDomainException) ex).getErrorCode())
                        .isEqualTo(AuthErrorCode.INVALID_REFRESH_TOKEN));
    }

    private User createVerifiedUser(String email) {
        registrationService.registerGuest(email, "Passw0rd!");
        EmailVerificationToken token = emailVerificationTokenRepository.findAll().stream()
                .filter(t -> email.equals(t.getUser().getEmail()))
                .findFirst()
                .orElseThrow();
        registrationService.verifyEmailToken(token.getToken());
        return userRepository.findByEmail(email).orElseThrow();
    }
}