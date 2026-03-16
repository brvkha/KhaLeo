package com.khaleo.flashcard.controller.deck.dto;

import com.khaleo.flashcard.entity.Deck;
import java.time.Instant;
import java.util.UUID;

public record DeckResponse(
        UUID id,
        UUID authorId,
        String name,
        String description,
        String coverImageUrl,
        String tags,
        Boolean isPublic,
        Instant createdAt,
        Instant updatedAt) {

    public static DeckResponse from(Deck deck) {
        return new DeckResponse(
                deck.getId(),
                deck.getAuthor().getId(),
                deck.getName(),
                deck.getDescription(),
                deck.getCoverImageUrl(),
                deck.getTags(),
                deck.getIsPublic(),
                deck.getCreatedAt(),
                deck.getUpdatedAt());
    }
}
