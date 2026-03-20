package com.khaleo.flashcard.controller.deck.dto;

import java.util.List;
import java.util.UUID;

public record PublicDeckDetailResponse(
        UUID id,
        String name,
        String description,
        String ownerName,
        List<String> tags,
        int cardCount,
        List<CardPreviewItem> cardsPreview) {

    public record CardPreviewItem(
            UUID id,
            String frontText) {
    }
}
