package com.khaleo.flashcard.controller.deck.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PublicDeckSummaryResponse(
        UUID id,
        String name,
        String ownerName,
        String description,
        List<String> tags,
        int cardCount,
        Instant updatedAt) {
}
