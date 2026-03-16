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
import com.khaleo.flashcard.service.auth.RegistrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class AuthRegistrationVerificationIT extends IntegrationPersistenceTestBase {

    @Autowired
    private RegistrationService registrationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailVerificationTokenRepository tokenRepository;

    @MockBean
    private com.khaleo.flashcard.service.auth.SesEmailService sesEmailService;

    @BeforeEach
    void setUp() {
        doNothing().when(sesEmailService).sendVerificationEmail(anyString(), anyString());
    }

    @Test
    void shouldRegisterUnverifiedThenVerifyWithSingleUseToken() {
        registrationService.registerGuest("flow-user@example.com", "Passw0rd!");

        User created = userRepository.findByEmail("flow-user@example.com").orElseThrow();
        assertThat(created.getIsEmailVerified()).isFalse();

        EmailVerificationToken token = tokenRepository.findAll().stream()
                .filter(t -> t.getUser().getId().equals(created.getId()))
                .findFirst()
                .orElseThrow();

        registrationService.verifyEmailToken(token.getToken());

        User verified = userRepository.findByEmail("flow-user@example.com").orElseThrow();
        assertThat(verified.getIsEmailVerified()).isTrue();

        assertThatThrownBy(() -> registrationService.verifyEmailToken(token.getToken()))
                .isInstanceOf(AuthDomainException.class)
                .satisfies(ex -> assertThat(((AuthDomainException) ex).getErrorCode())
                        .isEqualTo(AuthErrorCode.INVALID_VERIFICATION_TOKEN));
    }
}
