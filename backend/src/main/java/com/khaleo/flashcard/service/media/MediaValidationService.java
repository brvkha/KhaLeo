package com.khaleo.flashcard.service.media;

import com.khaleo.flashcard.service.persistence.PersistenceValidationException;
import com.khaleo.flashcard.service.persistence.PersistenceValidationException.PersistenceErrorCode;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class MediaValidationService {

    private static final Map<String, Set<String>> EXTENSIONS_BY_TYPE = new HashMap<>();

    static {
        EXTENSIONS_BY_TYPE.put("image/jpeg", Set.of("jpg", "jpeg"));
        EXTENSIONS_BY_TYPE.put("image/png", Set.of("png"));
        EXTENSIONS_BY_TYPE.put("audio/mpeg", Set.of("mp3"));
        EXTENSIONS_BY_TYPE.put("audio/webm", Set.of("webm"));
    }

    @Value("${app.media.s3.max-size-bytes:5242880}")
    private long maxSizeBytes;

    @Value("${app.media.authz.allowed-content-types:image/jpeg,image/png,audio/mpeg,audio/webm}")
    private String allowedContentTypes;

    public void validate(String fileName, String contentType, long sizeBytes) {
        if (fileName == null || fileName.isBlank()) {
            throw new PersistenceValidationException(PersistenceErrorCode.MEDIA_EXTENSION_MISMATCH, "fileName is required");
        }
        if (contentType == null || contentType.isBlank()) {
            throw new PersistenceValidationException(PersistenceErrorCode.MEDIA_TYPE_NOT_ALLOWED, "contentType is required");
        }

        if (sizeBytes <= 0 || sizeBytes > maxSizeBytes) {
            throw new PersistenceValidationException(
                    PersistenceErrorCode.MEDIA_SIZE_EXCEEDED,
                    "sizeBytes must be between 1 and " + maxSizeBytes);
        }

        Set<String> allowed = parseAllowedContentTypes();
        if (!allowed.contains(contentType)) {
            throw new PersistenceValidationException(
                    PersistenceErrorCode.MEDIA_TYPE_NOT_ALLOWED,
                    "Unsupported media content type: " + contentType);
        }

        String extension = extractExtension(fileName);
        Set<String> validExtensions = EXTENSIONS_BY_TYPE.getOrDefault(contentType, Set.of());
        if (!validExtensions.contains(extension.toLowerCase(Locale.ROOT))) {
            throw new PersistenceValidationException(
                    PersistenceErrorCode.MEDIA_EXTENSION_MISMATCH,
                    "File extension does not match content type.");
        }
    }

    private Set<String> parseAllowedContentTypes() {
        return java.util.Arrays.stream(allowedContentTypes.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .collect(Collectors.toSet());
    }

    private String extractExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dot + 1);
    }
}
