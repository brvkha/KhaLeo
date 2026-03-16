package com.khaleo.flashcard.unit.study;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.khaleo.flashcard.model.study.StudyPaginationToken;
import org.junit.jupiter.api.Test;

class StudyPaginationTokenCodecTest {

    @Test
    void shouldEncodeAndDecodeOffset() {
        StudyPaginationToken token = new StudyPaginationToken(42);

        String encoded = token.encode();
        StudyPaginationToken decoded = StudyPaginationToken.from(encoded);

        assertThat(decoded.offset()).isEqualTo(42);
    }

    @Test
    void shouldTreatBlankTokenAsInitial() {
        StudyPaginationToken decoded = StudyPaginationToken.from("  ");

        assertThat(decoded.offset()).isZero();
    }

    @Test
    void shouldRejectMalformedToken() {
        assertThatThrownBy(() -> StudyPaginationToken.from("not-base64"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid continuation token");
    }
}
