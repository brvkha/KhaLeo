package com.khaleo.flashcard.service.media;

import com.khaleo.flashcard.config.observability.NewRelicDeckMediaInstrumentation;
import com.khaleo.flashcard.entity.MediaObjectReference;
import com.khaleo.flashcard.repository.MediaObjectReferenceRepository;
import java.time.Instant;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class MediaReferenceService {

    private final MediaObjectReferenceRepository mediaObjectReferenceRepository;
    private final S3Client s3Client;
    private final NewRelicDeckMediaInstrumentation instrumentation;

    @Value("${app.media.s3.bucket}")
    private String bucket;

    @Value("${app.media.s3.key-prefix:uploads}")
    private String keyPrefix;

    public void incrementReference(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return;
        }

        MediaObjectReference reference = mediaObjectReferenceRepository.findById(objectKey)
                .orElseGet(() -> MediaObjectReference.builder()
                        .objectKey(objectKey)
                        .referenceCount(0)
                        .lastReferencedAt(Instant.now())
                        .build());

        reference.setReferenceCount(reference.getReferenceCount() + 1);
        reference.setLastReferencedAt(Instant.now());
        mediaObjectReferenceRepository.save(reference);
    }

    public void decrementReference(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return;
        }

        mediaObjectReferenceRepository.findById(objectKey).ifPresent(reference -> {
            int nextCount = Math.max(0, reference.getReferenceCount() - 1);
            reference.setReferenceCount(nextCount);
            reference.setLastDereferencedAt(Instant.now());

            if (nextCount == 0) {
                log.info("event=media_delete_eligible objectKey={}", objectKey);
                deleteFromS3(extractObjectKey(objectKey));
            }

            mediaObjectReferenceRepository.save(reference);
        });
    }

    private void deleteFromS3(String objectKey) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(objectKey)
                    .build());
            instrumentation.recordMediaCleanup("deleted", Map.of("objectKey", objectKey));
        } catch (RuntimeException ex) {
            instrumentation.recordMediaCleanup("delete_failed", Map.of("objectKey", objectKey, "reason", ex.getClass().getSimpleName()));
            log.warn("event=media_delete_failed objectKey={} reason={}", objectKey, ex.getMessage());
        }
    }

    private String extractObjectKey(String value) {
        String trimmed = value.trim();
        int schemeIndex = trimmed.indexOf("://");
        if (schemeIndex < 0) {
            return trimmed;
        }

        int pathStart = trimmed.indexOf('/', schemeIndex + 3);
        if (pathStart < 0 || pathStart == trimmed.length() - 1) {
            return trimmed;
        }

        String path = trimmed.substring(pathStart + 1);
        int query = path.indexOf('?');
        if (query >= 0) {
            path = path.substring(0, query);
        }

        if (path.startsWith(keyPrefix + "/")) {
            return path;
        }

        return path;
    }
}
