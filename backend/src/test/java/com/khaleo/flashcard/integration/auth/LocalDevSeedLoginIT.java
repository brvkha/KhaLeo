package com.khaleo.flashcard.integration.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.khaleo.flashcard.service.auth.AuthenticationService;
import com.khaleo.flashcard.integration.support.IntegrationPersistenceTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {
        "app.seed.local-dev.enabled=true",
        "app.seed.local-dev.default-password=khaleo"
})
@Transactional
@SuppressWarnings("null")
class LocalDevSeedLoginIT extends IntegrationPersistenceTestBase {

    @Autowired
    private AuthenticationService authenticationService;

    @Test
    void shouldAllowLoginForDefaultSeededAccount() {
        AuthenticationService.LoginResult loginResult = authenticationService.login("khaleo@khaleo.app", "khaleo");
        assertThat(loginResult.accessToken()).isNotBlank();
        assertThat(loginResult.refreshToken()).isNotBlank();
    }
}
