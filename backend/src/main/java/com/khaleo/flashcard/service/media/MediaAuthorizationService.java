package com.khaleo.flashcard.service.media;

import com.khaleo.flashcard.config.observability.NewRelicDeckMediaInstrumentation;
import com.khaleo.flashcard.entity.MediaUploadAuthorization;
import com.khaleo.flashcard.entity.MediaUploadAuthorization.AuthorizationStatus;
import com.khaleo.flashcard.entity.User;
import com.khaleo.flashcard.repository.MediaUploadAuthorizationRepository;
import com.khaleo.flashcard.repository.UserRepository;
import com.khaleo.flashcard.service.persistence.PersistenceValidationException;
import com.khaleo.flashcard.service.persistence.PersistenceValidationException.PersistenceErrorCode;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
@SuppressWarnings("null")
public class MediaAuthorizationService {

    private final UserRepository userRepository;
    private final MediaUploadAuthorizationRepository mediaUploadAuthorizationRepository;
    private final MediaAuthorizationRateLimiter mediaAuthorizationRateLimiter;
    private final S3PresignedUrlService s3PresignedUrlService;
    private final NewRelicDeckMediaInstrumentation instrumentation;

    @Value("${app.media.s3.max-size-bytes:5242880}")
    private long maxSizeBytes;

    public S3PresignedUrlService.PresignedUpload authorize(UUID userId, String fileName, String contentType, long sizeBytes) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new PersistenceValidationException(PersistenceErrorCode.MISSING_RELATIONSHIP, "Missing user: " + userId));

        try {
            mediaAuthorizationRateLimiter.ensureWithinLimit(userId);
            S3PresignedUrlService.PresignedUpload upload = s3PresignedUrlService.authorize(fileName, contentType, sizeBytes);

            mediaUploadAuthorizationRepository.save(MediaUploadAuthorization.builder()
                    .user(user)
                    .objectKey(upload.objectKey())
                    .contentType(contentType)
                    .maxSizeBytes(maxSizeBytes)
                    .issuedAt(Instant.now())
                    .expiresAt(upload.expiresAt())
                    .status(AuthorizationStatus.ISSUED)
                    .build());

            instrumentation.recordMediaAuthOutcome("issued", Map.of("userId", userId, "objectKey", upload.objectKey()));
            return upload;
        } catch (PersistenceValidationException ex) {
            persistRejection(user, fileName, contentType, ex);
            throw ex;
        }
    }

    private void persistRejection(User user, String fileName, String contentType, PersistenceValidationException ex) {
        AuthorizationStatus status = mapStatus(ex.getErrorCode());
        String objectKey = "rejected/" + UUID.randomUUID() + "-" + sanitize(fileName);

        mediaUploadAuthorizationRepository.save(MediaUploadAuthorization.builder()
                .user(user)
                .objectKey(objectKey)
                .contentType(contentType == null ? "unknown" : contentType)
                .maxSizeBytes(maxSizeBytes)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now())
                .status(status)
                .rejectionReason(ex.getMessage())
                .build());

        instrumentation.recordMediaAuthOutcome("rejected", Map.of("userId", user.getId(), "code", ex.getErrorCode().name()));
    }

    private AuthorizationStatus mapStatus(PersistenceErrorCode code) {
        return switch (code) {
            case MEDIA_AUTH_RATE_LIMIT_EXCEEDED -> AuthorizationStatus.REJECTED_RATE_LIMIT;
            case MEDIA_SIZE_EXCEEDED -> AuthorizationStatus.REJECTED_SIZE;
            default -> AuthorizationStatus.REJECTED_TYPE;
        };
    }

    private String sanitize(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "unknown";
        }
        return fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
