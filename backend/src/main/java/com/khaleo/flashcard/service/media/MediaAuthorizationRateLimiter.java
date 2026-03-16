package com.khaleo.flashcard.service.media;

import com.khaleo.flashcard.service.persistence.PersistenceValidationException;
import com.khaleo.flashcard.service.persistence.PersistenceValidationException.PersistenceErrorCode;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MediaAuthorizationRateLimiter {

    private final com.khaleo.flashcard.repository.MediaUploadAuthorizationRepository mediaUploadAuthorizationRepository;

    @Value("${app.media.authz.requests-per-minute:30}")
    private int requestsPerMinute;

    public void ensureWithinLimit(java.util.UUID userId) {
        Instant threshold = Instant.now().minusSeconds(60);
        long recent = mediaUploadAuthorizationRepository.countByUserIdAndIssuedAtAfter(userId, threshold);
        if (recent >= requestsPerMinute) {
            throw new PersistenceValidationException(
                    PersistenceErrorCode.MEDIA_AUTH_RATE_LIMIT_EXCEEDED,
                    "Media authorization request limit exceeded.");
        }
    }
}
