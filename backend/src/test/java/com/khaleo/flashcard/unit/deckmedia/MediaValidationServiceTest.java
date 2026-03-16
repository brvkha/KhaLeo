package com.khaleo.flashcard.unit.deckmedia;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.khaleo.flashcard.service.media.MediaValidationService;
import com.khaleo.flashcard.service.persistence.PersistenceValidationException;
import com.khaleo.flashcard.service.persistence.PersistenceValidationException.PersistenceErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class MediaValidationServiceTest {

    private MediaValidationService mediaValidationService;

    @BeforeEach
    void setUp() {
        mediaValidationService = new MediaValidationService();
        ReflectionTestUtils.setField(mediaValidationService, "maxSizeBytes", 5_242_880L);
        ReflectionTestUtils.setField(mediaValidationService, "allowedContentTypes", "image/jpeg,image/png,audio/mpeg,audio/webm");
    }

    @Test
    void shouldAcceptValidTypeExtensionAndSize() {
        assertThatCode(() -> mediaValidationService.validate("clip.mp3", "audio/mpeg", 1024)).doesNotThrowAnyException();
    }

    @Test
    void shouldRejectUnsupportedType() {
        assertThatThrownBy(() -> mediaValidationService.validate("clip.exe", "application/octet-stream", 1024))
                .isInstanceOf(PersistenceValidationException.class)
                .satisfies(ex -> org.assertj.core.api.Assertions.assertThat(((PersistenceValidationException) ex).getErrorCode())
                        .isEqualTo(PersistenceErrorCode.MEDIA_TYPE_NOT_ALLOWED));
    }

    @Test
    void shouldRejectSizeBeyondLimit() {
        assertThatThrownBy(() -> mediaValidationService.validate("image.jpg", "image/jpeg", 5_242_881L))
                .isInstanceOf(PersistenceValidationException.class)
                .satisfies(ex -> org.assertj.core.api.Assertions.assertThat(((PersistenceValidationException) ex).getErrorCode())
                        .isEqualTo(PersistenceErrorCode.MEDIA_SIZE_EXCEEDED));
    }

    @Test
    void shouldRejectMismatchedExtension() {
        assertThatThrownBy(() -> mediaValidationService.validate("image.png", "audio/mpeg", 1000))
                .isInstanceOf(PersistenceValidationException.class)
                .satisfies(ex -> org.assertj.core.api.Assertions.assertThat(((PersistenceValidationException) ex).getErrorCode())
                        .isEqualTo(PersistenceErrorCode.MEDIA_EXTENSION_MISMATCH));
    }
}
