package com.khaleo.flashcard.unit.deckmedia;

import static org.assertj.core.api.Assertions.assertThat;

import com.khaleo.flashcard.controller.card.dto.CardSearchQuery;
import org.junit.jupiter.api.Test;

class CardSearchCriteriaTest {

    @Test
    void shouldTrimValuesAndConvertBlanksToNull() {
        CardSearchQuery normalized = new CardSearchQuery("  hello  ", "   ", "  apple ", 0, 20).normalized();

        assertThat(normalized.frontText()).isEqualTo("hello");
        assertThat(normalized.backText()).isNull();
        assertThat(normalized.vocabulary()).isEqualTo("apple");
    }
}
