package com.khaleo.flashcard.integration.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;

import com.khaleo.flashcard.entity.EmailVerificationToken;
import com.khaleo.flashcard.entity.User;
import com.khaleo.flashcard.integration.support.IntegrationPersistenceTestBase;
import com.khaleo.flashcard.repository.EmailVerificationTokenRepository;
import com.khaleo.flashcard.repository.UserRepository;
import com.khaleo.flashcard.service.auth.AuthDomainException;
import com.khaleo.flashcard.service.auth.AuthErrorCode;
import com.khaleo.flashcard.service.auth.AuthenticationService;
import com.khaleo.flashcard.service.auth.RegistrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class LoginLockoutPolicyIT extends IntegrationPersistenceTestBase {

    @Autowired
    private RegistrationService registrationService;

    @Autowired
    private AuthenticationService authenticationService;

    @Autowired
    private EmailVerificationTokenRepository emailVerificationTokenRepository;

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
    void shouldLockAccountAfterFiveConsecutiveFailures() {
        registrationService.registerGuest("lockout@example.com", "Passw0rd!");
        EmailVerificationToken token = emailVerificationTokenRepository.findAll().stream()
                .filter(t -> "lockout@example.com".equals(t.getUser().getEmail()))
                .findFirst()
                .orElseThrow();
        registrationService.verifyEmailToken(token.getToken());

        for (int i = 0; i < 4; i++) {
            assertThatThrownBy(() -> authenticationService.authenticateVerifiedUser("lockout@example.com", "wrong-pass"))
                    .isInstanceOf(AuthDomainException.class)
                    .satisfies(ex -> assertThat(((AuthDomainException) ex).getErrorCode())
                            .isEqualTo(AuthErrorCode.INVALID_CREDENTIALS));
        }

        assertThatThrownBy(() -> authenticationService.authenticateVerifiedUser("lockout@example.com", "wrong-pass"))
                .isInstanceOf(AuthDomainException.class)
                .satisfies(ex -> assertThat(((AuthDomainException) ex).getErrorCode())
                        .isEqualTo(AuthErrorCode.INVALID_CREDENTIALS));

        assertThatThrownBy(() -> authenticationService.authenticateVerifiedUser("lockout@example.com", "Passw0rd!"))
                .isInstanceOf(AuthDomainException.class)
                .satisfies(ex -> assertThat(((AuthDomainException) ex).getErrorCode())
                        .isEqualTo(AuthErrorCode.ACCOUNT_LOCKED));

        User lockedUser = userRepository.findByEmail("lockout@example.com").orElseThrow();
        assertThat(lockedUser.getAccountLockedUntil()).isNotNull();
    }
}