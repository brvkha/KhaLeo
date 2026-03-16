package com.khaleo.flashcard.service.media;

import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

@Service
@RequiredArgsConstructor
public class S3PresignedUrlService {

    private final S3Presigner s3Presigner;
    private final MediaValidationService mediaValidationService;

    @Value("${app.media.s3.bucket}")
    private String bucket;

    @Value("${app.media.s3.key-prefix:uploads}")
    private String keyPrefix;

    @Value("${app.media.s3.presigned-url-ttl-seconds:300}")
    private long ttlSeconds;

    public PresignedUpload authorize(String fileName, String contentType, long sizeBytes) {
        mediaValidationService.validate(fileName, contentType, sizeBytes);

        String objectKey = buildObjectKey(fileName);
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .contentType(contentType)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(ttlSeconds))
                .putObjectRequest(putObjectRequest)
                .build();

        PresignedPutObjectRequest presigned = s3Presigner.presignPutObject(presignRequest);
        URL url = presigned.url();

        return new PresignedUpload(objectKey, url.toString(), ttlSeconds, Instant.now().plusSeconds(ttlSeconds));
    }

    private String buildObjectKey(String fileName) {
        String safeName = fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
        String datePath = java.time.LocalDate.now().toString();
        return keyPrefix + "/" + datePath + "/" + UUID.randomUUID() + "-" + safeName;
    }

    public record PresignedUpload(
            String objectKey,
            String uploadUrl,
            long expiresInSeconds,
            Instant expiresAt) {
    }
}
