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
import com.khaleo.flashcard.service.auth.AuthenticationService;
import com.khaleo.flashcard.service.auth.PasswordResetService;
import com.khaleo.flashcard.service.auth.RegistrationService;
import com.khaleo.flashcard.service.auth.TokenRefreshService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class PasswordResetRevokesSessionsIT extends IntegrationPersistenceTestBase {

    @Autowired
    private RegistrationService registrationService;

    @Autowired
    private AuthenticationService authenticationService;

    @Autowired
    private PasswordResetService passwordResetService;

    @Autowired
    private TokenRefreshService tokenRefreshService;

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
    void shouldInvalidateExistingRefreshTokensAfterPasswordReset() {
        registrationService.registerGuest("revoke-sessions@example.com", "Passw0rd!");
        EmailVerificationToken verifyToken = emailVerificationTokenRepository.findAll().stream()
                .filter(t -> "revoke-sessions@example.com".equals(t.getUser().getEmail()))
                .findFirst()
                .orElseThrow();
        registrationService.verifyEmailToken(verifyToken.getToken());

        AuthenticationService.LoginResult login = authenticationService.login("revoke-sessions@example.com", "Passw0rd!");
        assertThat(login.refreshToken()).isNotBlank();

        passwordResetService.forgotPassword("revoke-sessions@example.com");
        User user = userRepository.findByEmail("revoke-sessions@example.com").orElseThrow();
        PasswordResetToken resetToken = passwordResetTokenRepository.findTopByUserIdOrderByCreatedAtDesc(user.getId())
                .orElseThrow();
        passwordResetService.resetPassword(resetToken.getToken(), "NewPassw0rd!");

        assertThatThrownBy(() -> tokenRefreshService.refreshAccessToken(login.refreshToken()))
                .isInstanceOf(AuthDomainException.class)
                .satisfies(ex -> assertThat(((AuthDomainException) ex).getErrorCode())
                        .isEqualTo(AuthErrorCode.INVALID_REFRESH_TOKEN));
    }
}