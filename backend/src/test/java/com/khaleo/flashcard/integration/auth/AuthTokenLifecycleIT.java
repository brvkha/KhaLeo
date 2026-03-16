package com.khaleo.flashcard.integration.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;

import com.khaleo.flashcard.entity.EmailVerificationToken;
import com.khaleo.flashcard.integration.support.IntegrationPersistenceTestBase;
import com.khaleo.flashcard.repository.EmailVerificationTokenRepository;
import com.khaleo.flashcard.service.auth.AuthDomainException;
import com.khaleo.flashcard.service.auth.AuthErrorCode;
import com.khaleo.flashcard.service.auth.AuthenticationService;
import com.khaleo.flashcard.service.auth.LogoutService;
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
class AuthTokenLifecycleIT extends IntegrationPersistenceTestBase {

    @Autowired
    private RegistrationService registrationService;

    @Autowired
    private AuthenticationService authenticationService;

    @Autowired
    private TokenRefreshService tokenRefreshService;

    @Autowired
    private LogoutService logoutService;

    @Autowired
    private EmailVerificationTokenRepository emailVerificationTokenRepository;

    @MockBean
    private com.khaleo.flashcard.service.auth.SesEmailService sesEmailService;

    @BeforeEach
    void setUp() {
        doNothing().when(sesEmailService).sendVerificationEmail(anyString(), anyString());
        doNothing().when(sesEmailService).sendPasswordResetEmail(anyString(), anyString());
    }

    @Test
    void shouldIssueRefreshAndAccessThenRefreshAndLogout() {
        registrationService.registerGuest("token-flow@example.com", "Passw0rd!");
        EmailVerificationToken token = emailVerificationTokenRepository.findAll().stream()
                .filter(t -> "token-flow@example.com".equals(t.getUser().getEmail()))
                .findFirst()
                .orElseThrow();
        registrationService.verifyEmailToken(token.getToken());

        AuthenticationService.LoginResult login = authenticationService.login("token-flow@example.com", "Passw0rd!");
        assertThat(login.accessToken()).isNotBlank();
        assertThat(login.refreshToken()).isNotBlank();

        TokenRefreshService.RefreshResult refreshed = tokenRefreshService.refreshAccessToken(login.refreshToken());
        assertThat(refreshed.accessToken()).isNotBlank();

        logoutService.logoutByRefreshToken(login.refreshToken());

        assertThatThrownBy(() -> tokenRefreshService.refreshAccessToken(login.refreshToken()))
                .isInstanceOf(AuthDomainException.class)
                .satisfies(ex -> assertThat(((AuthDomainException) ex).getErrorCode())
                        .isEqualTo(AuthErrorCode.INVALID_REFRESH_TOKEN));
    }
}