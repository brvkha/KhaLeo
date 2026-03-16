package com.khaleo.flashcard.integration.deckmedia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.khaleo.flashcard.entity.User;
import com.khaleo.flashcard.entity.enums.UserRole;
import com.khaleo.flashcard.integration.support.IntegrationPersistenceTestBase;
import com.khaleo.flashcard.repository.UserRepository;
import com.khaleo.flashcard.service.media.MediaAuthorizationService;
import com.khaleo.flashcard.service.media.S3PresignedUrlService;
import com.khaleo.flashcard.service.persistence.PersistenceValidationException;
import com.khaleo.flashcard.service.persistence.PersistenceValidationException.PersistenceErrorCode;
import java.net.URL;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

@SpringBootTest
@Transactional
@SuppressWarnings("null")
class MediaAuthorizationValidationIT extends IntegrationPersistenceTestBase {

    @Autowired
    private MediaAuthorizationService mediaAuthorizationService;

    @Autowired
    private UserRepository userRepository;

    @MockBean
    private S3Presigner s3Presigner;

    @MockBean
    private S3Client s3Client;

    @Test
    void shouldIssuePresignedUrlWithFiveMinuteExpiryForValidInput() throws Exception {
        User user = saveUser("it-media-owner@example.com");
        PresignedPutObjectRequest presigned = mock(PresignedPutObjectRequest.class);
        when(presigned.url()).thenReturn(new URL("https://example.com/upload"));
        when(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class))).thenReturn(presigned);

        S3PresignedUrlService.PresignedUpload response = mediaAuthorizationService.authorize(
                user.getId(),
                "clip.mp3",
                "audio/mpeg",
                1024);

        assertThat(response.uploadUrl()).isEqualTo("https://example.com/upload");
        assertThat(response.expiresInSeconds()).isEqualTo(300);
        Duration ttl = Duration.between(java.time.Instant.now(), response.expiresAt());
        assertThat(ttl.getSeconds()).isBetween(295L, 305L);
    }

    @Test
    void shouldRejectUnsupportedContentType() {
        User user = saveUser("it-media-owner-2@example.com");

        assertThatThrownBy(() -> mediaAuthorizationService.authorize(
                user.getId(),
                "clip.exe",
                "application/octet-stream",
                1024))
                .isInstanceOf(PersistenceValidationException.class)
                .satisfies(ex -> assertThat(((PersistenceValidationException) ex).getErrorCode())
                        .isEqualTo(PersistenceErrorCode.MEDIA_TYPE_NOT_ALLOWED));
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
