package com.khaleo.flashcard.contract.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.khaleo.flashcard.controller.auth.dto.RegisterRequest;
import com.khaleo.flashcard.entity.EmailVerificationToken;
import com.khaleo.flashcard.integration.support.IntegrationPersistenceTestBase;
import com.khaleo.flashcard.repository.EmailVerificationTokenRepository;
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
class AuthRegistrationVerificationContractTest extends IntegrationPersistenceTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EmailVerificationTokenRepository emailVerificationTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @MockBean
    private SesEmailService sesEmailService;

    @BeforeEach
    void setUp() {
        doNothing().when(sesEmailService).sendVerificationEmail(anyString(), anyString());
    }

    @Test
    void shouldRegisterGuestAndReturnCreated() throws Exception {
        RegisterRequest request = new RegisterRequest("contract-user@example.com", "Passw0rd!");

        String response = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(response).contains("verificationRequired", "true");
        assertThat(userRepository.findByEmail("contract-user@example.com")).isPresent();
    }

    @Test
    void shouldVerifyTokenSuccessfully() throws Exception {
        RegisterRequest request = new RegisterRequest("verify-me@example.com", "Passw0rd!");
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        EmailVerificationToken token = emailVerificationTokenRepository.findAll().stream()
                .filter(t -> "verify-me@example.com".equals(t.getUser().getEmail()))
                .findFirst()
                .orElseThrow();

        mockMvc.perform(get("/api/v1/auth/verify").queryParam("token", token.getToken()))
                .andExpect(status().isOk());

        assertThat(userRepository.findByEmail("verify-me@example.com")).isPresent();
        assertThat(userRepository.findByEmail("verify-me@example.com").orElseThrow().getIsEmailVerified())
                .isTrue();
    }
}
