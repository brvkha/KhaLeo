package com.khaleo.flashcard.contract.deckmedia;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.khaleo.flashcard.entity.User;
import com.khaleo.flashcard.entity.enums.UserRole;
import com.khaleo.flashcard.integration.support.IntegrationPersistenceTestBase;
import com.khaleo.flashcard.repository.UserRepository;
import com.khaleo.flashcard.service.auth.JwtTokenService;
import com.khaleo.flashcard.service.media.MediaAuthorizationService;
import com.khaleo.flashcard.service.media.S3PresignedUrlService;
import com.khaleo.flashcard.service.persistence.PersistenceValidationException;
import com.khaleo.flashcard.service.persistence.PersistenceValidationException.PersistenceErrorCode;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@SuppressWarnings("null")
class MediaAuthorizationContractTest extends IntegrationPersistenceTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockBean
    private MediaAuthorizationService mediaAuthorizationService;

    @Test
    void shouldReturnPresignedUploadContractPayload() throws Exception {
        User user = createUser("contract-media-owner@example.com");

        when(mediaAuthorizationService.authorize(any(), anyString(), anyString(), anyLong()))
                .thenReturn(new S3PresignedUrlService.PresignedUpload(
                        "uploads/2026-03-16/abc-file.jpg",
                        "https://example.com/presigned",
                        300,
                        Instant.now().plusSeconds(300)));

        mockMvc.perform(get("/api/v1/media/presigned-url")
                        .header("Authorization", bearerFor(user))
                        .queryParam("fileName", "file.jpg")
                        .queryParam("contentType", "image/jpeg")
                        .queryParam("sizeBytes", "1024"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.objectKey").value("uploads/2026-03-16/abc-file.jpg"))
                .andExpect(jsonPath("$.uploadUrl").value("https://example.com/presigned"))
                .andExpect(jsonPath("$.expiresInSeconds").value(300));
    }

    @Test
    void shouldReturnBadRequestOnValidationFailure() throws Exception {
        User user = createUser("contract-media-owner-2@example.com");

        when(mediaAuthorizationService.authorize(any(), anyString(), anyString(), anyLong()))
                .thenThrow(new PersistenceValidationException(
                        PersistenceErrorCode.MEDIA_TYPE_NOT_ALLOWED,
                        "Unsupported media content type"));

        mockMvc.perform(get("/api/v1/media/presigned-url")
                        .header("Authorization", bearerFor(user))
                        .queryParam("fileName", "file.exe")
                        .queryParam("contentType", "application/octet-stream")
                        .queryParam("sizeBytes", "1024"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MEDIA_TYPE_NOT_ALLOWED"));
    }

    private User createUser(String email) {
        return userRepository.saveAndFlush(User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode("Passw0rd!"))
                .role(UserRole.ROLE_USER)
                .isEmailVerified(true)
                .build());
    }

    private String bearerFor(User user) {
        return "Bearer " + jwtTokenService.createAccessToken(user.getId().toString(), Map.of("role", user.getRole().name()));
    }
}
