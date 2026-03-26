package com.khaleo.flashcard.model.study;

import com.khaleo.flashcard.entity.Card;
import com.khaleo.flashcard.entity.CardLearningState;
import com.khaleo.flashcard.entity.enums.CardLearningStateType;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record StudyCardSummary(
        UUID cardId,
        UUID deckId,
        String term,
        String answer,
        String imageUrl,
        String partOfSpeech,
        String phonetic,
        List<String> examples,
        String frontText,
        String backText,
        CardLearningStateType state,
        Instant nextReviewDate,
        String sourceTier) {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {};

    public static StudyCardSummary fromLearningState(CardLearningState state, String sourceTier) {
        Card card = state.getCard();
        return new StudyCardSummary(
                card.getId(),
                card.getDeck().getId(),
                card.getTerm(),
                card.getAnswer(),
                card.getImageUrl(),
                card.getPartOfSpeech(),
                card.getPhonetic(),
                parseExamples(card.getExamplesJson()),
                card.getFrontText(),
                card.getBackText(),
                state.getState(),
                state.getNextReviewDate(),
                sourceTier);
    }

    public static StudyCardSummary fromNewCard(Card card) {
        return new StudyCardSummary(
                card.getId(),
                card.getDeck().getId(),
                card.getTerm(),
                card.getAnswer(),
                card.getImageUrl(),
                card.getPartOfSpeech(),
                card.getPhonetic(),
                parseExamples(card.getExamplesJson()),
                card.getFrontText(),
                card.getBackText(),
                CardLearningStateType.NEW,
                null,
                "NEW");
    }

    private static List<String> parseExamples(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        try {
            return OBJECT_MAPPER.readValue(raw, STRING_LIST);
        } catch (Exception ex) {
            return List.of();
        }
    }
}
