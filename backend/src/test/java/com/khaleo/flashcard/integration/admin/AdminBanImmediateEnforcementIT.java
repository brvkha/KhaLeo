package com.khaleo.flashcard.integration.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.khaleo.flashcard.entity.User;
import com.khaleo.flashcard.entity.enums.UserRole;
import com.khaleo.flashcard.integration.support.IntegrationPersistenceTestBase;
import com.khaleo.flashcard.repository.UserRepository;
import com.khaleo.flashcard.service.auth.JwtTokenService;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@SuppressWarnings("null")
class AdminBanImmediateEnforcementIT extends IntegrationPersistenceTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenService jwtTokenService;

    private User admin;
    private User learner;

    @BeforeEach
    void setUp() {
        admin = userRepository.save(User.builder()
                .email("admin-enforce@example.com")
                .passwordHash("hash")
                .role(UserRole.ROLE_ADMIN)
                .isEmailVerified(true)
                .dailyLearningLimit(100)
                .build());

        learner = userRepository.save(User.builder()
                .email("learner-enforce@example.com")
                .passwordHash("hash")
                .role(UserRole.ROLE_USER)
                .isEmailVerified(true)
                .dailyLearningLimit(100)
                .build());
    }

    @Test
    void shouldBlockAuthenticatedRequestsImmediatelyAfterBanEvenWithPreIssuedToken() throws Exception {
        String preIssuedLearnerToken = tokenFor(learner);
        String adminToken = tokenFor(admin);

        mockMvc.perform(post("/api/v1/admin/users/{userId}/ban", learner.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        String createDeckBody = objectMapper.writeValueAsString(Map.of(
                "name", "Blocked Deck",
                "description", "should fail",
                "isPublic", false,
                "tags", "blocked"));

        String response = mockMvc.perform(post("/api/v1/decks")
                        .header("Authorization", "Bearer " + preIssuedLearnerToken)
                        .contentType("application/json")
                        .content(createDeckBody))
                .andExpect(status().isForbidden())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(response).contains("BANNED_USER_REQUEST_DENIED");
    }

    private String tokenFor(User user) {
        return jwtTokenService.createAccessToken(user.getId().toString(), Map.of("role", user.getRole().name()));
    }
}
