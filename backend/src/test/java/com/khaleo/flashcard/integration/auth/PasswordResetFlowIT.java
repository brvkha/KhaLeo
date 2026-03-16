package com.khaleo.flashcard.integration.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;

import com.khaleo.flashcard.entity.EmailVerificationToken;
import com.khaleo.flashcard.entity.PasswordResetToken;
import com.khaleo.flashcard.entity.User;
import com.khaleo.flashcard.integration.support.IntegrationPersistenceTestBase;
import com.khaleo.flashcard.repository.EmailVerificationTokenRepository;
import com.khaleo.flashcard.repository.PasswordResetTokenRepository;
import com.khaleo.flashcard.repository.UserRepository;
import com.khaleo.flashcard.service.auth.AuthDomainException;
import com.khaleo.flashcard.service.auth.AuthErrorCode;
import com.khaleo.flashcard.service.auth.PasswordResetService;
import com.khaleo.flashcard.service.auth.RegistrationService;
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
class PasswordResetFlowIT extends IntegrationPersistenceTestBase {

    @Autowired
    private RegistrationService registrationService;

    @Autowired
    private PasswordResetService passwordResetService;

    @Autowired
    private EmailVerificationTokenRepository emailVerificationTokenRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @MockBean
    private com.khaleo.flashcard.service.auth.SesEmailService sesEmailService;

    @BeforeEach
    void setUp() {
        doNothing().when(sesEmailService).sendVerificationEmail(anyString(), anyString());
        doNothing().when(sesEmailService).sendPasswordResetEmail(anyString(), anyString());
    }

    @Test
    void shouldResetPasswordAndEnforceSingleUseAndExpiry() {
        registrationService.registerGuest("reset-flow@example.com", "Passw0rd!");
        EmailVerificationToken verifyToken = emailVerificationTokenRepository.findAll().stream()
                .filter(t -> "reset-flow@example.com".equals(t.getUser().getEmail()))
                .findFirst()
                .orElseThrow();
        registrationService.verifyEmailToken(verifyToken.getToken());

        passwordResetService.forgotPassword("reset-flow@example.com");
        User user = userRepository.findByEmail("reset-flow@example.com").orElseThrow();
        PasswordResetToken token = passwordResetTokenRepository.findTopByUserIdOrderByCreatedAtDesc(user.getId())
                .orElseThrow();

        passwordResetService.resetPassword(token.getToken(), "NewPassw0rd!");

        assertThatThrownBy(() -> passwordResetService.resetPassword(token.getToken(), "AnotherPassw0rd!"))
                .isInstanceOf(AuthDomainException.class)
                .satisfies(ex -> assertThat(((AuthDomainException) ex).getErrorCode())
                        .isEqualTo(AuthErrorCode.INVALID_RESET_TOKEN));

        String expiredTokenValue = UUID.randomUUID().toString();
        passwordResetTokenRepository.save(PasswordResetToken.builder()
                .token(expiredTokenValue)
                .user(user)
                .expiresAt(Instant.now().minusSeconds(5))
                .build());

        assertThatThrownBy(() -> passwordResetService.resetPassword(expiredTokenValue, "AnotherPassw0rd!"))
                .isInstanceOf(AuthDomainException.class)
                .satisfies(ex -> assertThat(((AuthDomainException) ex).getErrorCode())
                        .isEqualTo(AuthErrorCode.EXPIRED_RESET_TOKEN));
    }
}