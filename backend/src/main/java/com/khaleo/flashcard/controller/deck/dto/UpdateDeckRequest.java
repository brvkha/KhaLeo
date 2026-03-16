package com.khaleo.flashcard.controller.deck.dto;

import jakarta.validation.constraints.Size;

public record UpdateDeckRequest(
        @Size(max = 100) String name,
        String description,
        @Size(max = 2048) String coverImageUrl,
        Boolean isPublic,
        String tags) {
}
