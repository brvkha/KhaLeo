package com.khaleo.flashcard.integration.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;

import com.khaleo.flashcard.integration.support.IntegrationPersistenceTestBase;
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
class UnverifiedLoginBlockedIT extends IntegrationPersistenceTestBase {

    @Autowired
    private RegistrationService registrationService;

    @Autowired
    private AuthenticationService authenticationService;

    @BeforeEach
    void setUp() {
        // No need to mock - MockEmailService is now @Primary and logs emails instead
    }

    @Test
    void shouldRejectLoginForUnverifiedAccount() {
        registrationService.registerGuest("blocked@example.com", "Passw0rd!");

        assertThatThrownBy(() -> authenticationService.authenticateVerifiedUser("blocked@example.com", "Passw0rd!"))
                .isInstanceOf(AuthDomainException.class)
                .satisfies(ex -> assertThat(((AuthDomainException) ex).getErrorCode())
                        .isEqualTo(AuthErrorCode.UNVERIFIED_EMAIL));
    }
}
