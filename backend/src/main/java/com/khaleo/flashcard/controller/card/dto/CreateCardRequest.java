package com.khaleo.flashcard.controller.card.dto;

import jakarta.validation.constraints.Size;

public record CreateCardRequest(
        String frontText,
        @Size(max = 2048) String frontMediaUrl,
        String backText,
        @Size(max = 2048) String backMediaUrl) {
}
