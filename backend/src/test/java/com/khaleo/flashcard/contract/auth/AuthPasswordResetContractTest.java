package com.khaleo.flashcard.contract.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.khaleo.flashcard.controller.auth.dto.ForgotPasswordRequest;
import com.khaleo.flashcard.controller.auth.dto.RegisterRequest;
import com.khaleo.flashcard.controller.auth.dto.ResetPasswordRequest;
import com.khaleo.flashcard.entity.EmailVerificationToken;
import com.khaleo.flashcard.entity.PasswordResetToken;
import com.khaleo.flashcard.entity.User;
import com.khaleo.flashcard.integration.support.IntegrationPersistenceTestBase;
import com.khaleo.flashcard.repository.EmailVerificationTokenRepository;
import com.khaleo.flashcard.repository.PasswordResetTokenRepository;
import com.khaleo.flashcard.repository.UserRepository;
import com.khaleo.flashcard.service.auth.SesEmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@SuppressWarnings("null")
class AuthPasswordResetContractTest extends IntegrationPersistenceTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailVerificationTokenRepository emailVerificationTokenRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @MockBean
    private SesEmailService sesEmailService;

    @BeforeEach
    void setUp() {
        doNothing().when(sesEmailService).sendVerificationEmail(anyString(), anyString());
        doNothing().when(sesEmailService).sendPasswordResetEmail(anyString(), anyString());
    }

    @Test
    void shouldAcceptForgotPasswordWithoutEnumeration() throws Exception {
        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new ForgotPasswordRequest("unknown@example.com"))))
                .andExpect(status().isAccepted());
    }

    @Test
    void shouldResetPasswordWithValidToken() throws Exception {
        verifyUser("reset-contract@example.com");

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new ForgotPasswordRequest("reset-contract@example.com"))))
                .andExpect(status().isAccepted());

        User user = userRepository.findByEmail("reset-contract@example.com").orElseThrow();
        PasswordResetToken token = passwordResetTokenRepository.findTopByUserIdOrderByCreatedAtDesc(user.getId()).orElseThrow();

        String response = mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new ResetPasswordRequest(token.getToken(), "NewPassw0rd!"))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(response).contains("\"success\":true");
    }

    private void verifyUser(String email) throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new RegisterRequest(email, "Passw0rd!"))))
                .andExpect(status().isCreated());

        EmailVerificationToken token = emailVerificationTokenRepository.findAll().stream()
                .filter(t -> email.equals(t.getUser().getEmail()))
                .findFirst()
                .orElseThrow();

        mockMvc.perform(get("/api/v1/auth/verify").queryParam("token", token.getToken()))
                .andExpect(status().isOk());
    }
}