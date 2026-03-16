package com.khaleo.flashcard.integration.deckmedia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.khaleo.flashcard.entity.MediaUploadAuthorization;
import com.khaleo.flashcard.entity.User;
import com.khaleo.flashcard.entity.enums.UserRole;
import com.khaleo.flashcard.integration.support.IntegrationPersistenceTestBase;
import com.khaleo.flashcard.repository.MediaUploadAuthorizationRepository;
import com.khaleo.flashcard.repository.UserRepository;
import com.khaleo.flashcard.service.media.MediaAuthorizationService;
import com.khaleo.flashcard.service.persistence.PersistenceValidationException;
import com.khaleo.flashcard.service.persistence.PersistenceValidationException.PersistenceErrorCode;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@SpringBootTest
@Transactional
@SuppressWarnings("null")
class MediaAuthorizationRateLimitIT extends IntegrationPersistenceTestBase {

    @Autowired
    private MediaAuthorizationService mediaAuthorizationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MediaUploadAuthorizationRepository mediaUploadAuthorizationRepository;

    @MockBean
    private S3Presigner s3Presigner;

    @MockBean
    private S3Client s3Client;

    @Test
    void shouldRejectRequestWhenRateLimitExceeded() {
        User user = saveUser("it-rate-limit-owner@example.com");

        for (int i = 0; i < 30; i++) {
            mediaUploadAuthorizationRepository.save(MediaUploadAuthorization.builder()
                    .user(user)
                    .objectKey("uploads/key-" + i)
                    .contentType("image/jpeg")
                    .maxSizeBytes(1024L)
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(300))
                    .status(MediaUploadAuthorization.AuthorizationStatus.ISSUED)
                    .build());
        }

        assertThatThrownBy(() -> mediaAuthorizationService.authorize(
                user.getId(),
                "file.jpg",
                "image/jpeg",
                1024))
                .isInstanceOf(PersistenceValidationException.class)
                .satisfies(ex -> assertThat(((PersistenceValidationException) ex).getErrorCode())
                        .isEqualTo(PersistenceErrorCode.MEDIA_AUTH_RATE_LIMIT_EXCEEDED));
    }

    private User saveUser(String email) {
        return userRepository.saveAndFlush(User.builder()
                .email(email)
                .passwordHash("hash")
                .role(UserRole.ROLE_USER)
                .isEmailVerified(true)
                .build());
    }
}
