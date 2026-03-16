package com.khaleo.flashcard.controller.card.dto;

import com.khaleo.flashcard.entity.Card;
import java.time.Instant;
import java.util.UUID;

public record CardResponse(
        UUID id,
        UUID deckId,
        String frontText,
        String frontMediaUrl,
        String backText,
        String backMediaUrl,
        Instant createdAt,
        Instant updatedAt) {

    public static CardResponse from(Card card) {
        return new CardResponse(
                card.getId(),
                card.getDeck().getId(),
                card.getFrontText(),
                card.getFrontMediaUrl(),
                card.getBackText(),
                card.getBackMediaUrl(),
                card.getCreatedAt(),
                card.getUpdatedAt());
    }
}
