package com.khaleo.flashcard.integration.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.khaleo.flashcard.entity.User;
import com.khaleo.flashcard.entity.enums.UserRole;
import com.khaleo.flashcard.integration.support.IntegrationPersistenceTestBase;
import com.khaleo.flashcard.repository.UserRepository;
import com.khaleo.flashcard.service.auth.JwtTokenService;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {
        "app.observability.splunk.enabled=true",
        "app.observability.splunk.hec-url=http://127.0.0.1:9/services/collector/event",
        "app.observability.splunk.hec-token=test-token"
})
@AutoConfigureMockMvc
@Transactional
@SuppressWarnings("null")
class SplunkAsyncNonBlockingIT extends IntegrationPersistenceTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenService jwtTokenService;

    private User admin;
    private User learner;

    @BeforeEach
    void setUp() {
        admin = userRepository.save(User.builder()
                .email("admin-obs@example.com")
                .passwordHash("hash")
                .role(UserRole.ROLE_ADMIN)
                .isEmailVerified(true)
                .dailyLearningLimit(100)
                .build());

        learner = userRepository.save(User.builder()
                .email("learner-obs@example.com")
                .passwordHash("hash")
                .role(UserRole.ROLE_USER)
                .isEmailVerified(true)
                .dailyLearningLimit(100)
                .build());
    }

    @Test
    void shouldKeepAdminBanEndpointResponsiveWhenSplunkDeliveryFails() throws Exception {
        String adminToken = tokenFor(admin);

        Instant started = Instant.now();
        mockMvc.perform(post("/api/v1/admin/users/{userId}/ban", learner.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
        Duration elapsed = Duration.between(started, Instant.now());

        assertThat(elapsed.toMillis()).isLessThan(5000);
    }

    private String tokenFor(User user) {
        return jwtTokenService.createAccessToken(user.getId().toString(), Map.of("role", user.getRole().name()));
    }
}
