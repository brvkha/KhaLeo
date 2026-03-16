package com.khaleo.flashcard.controller.admin.dto;

import jakarta.validation.constraints.Size;

public record AdminCardUpdateRequest(
        @Size(max = 5000) String frontText,
        @Size(max = 2048) String frontMediaUrl,
        @Size(max = 5000) String backText,
        @Size(max = 2048) String backMediaUrl) {
}
