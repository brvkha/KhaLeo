package com.khaleo.flashcard.controller.card.dto;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.khaleo.flashcard.entity.Card;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CardResponse(
        UUID id,
        UUID deckId,
        String term,
        String answer,
        String imageUrl,
        String partOfSpeech,
        String phonetic,
        List<String> examples,
        Long version,
        String frontText,
        String backText,
        Instant createdAt,
        Instant updatedAt) {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {};

    public static CardResponse from(Card card) {
        List<String> examples = parseExamples(card.getExamplesJson());
        return new CardResponse(
                card.getId(),
                card.getDeck().getId(),
                card.getTerm(),
                card.getAnswer(),
                card.getImageUrl(),
                card.getPartOfSpeech(),
                card.getPhonetic(),
                examples,
                card.getVersion(),
                card.getFrontText(),
                card.getBackText(),
                card.getCreatedAt(),
                card.getUpdatedAt());
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
