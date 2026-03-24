package com.khaleo.flashcard.controller.admin.dto;

import java.time.Instant;
import java.util.UUID;

public record AdminDeckModerationItemResponse(
        UUID id,
        String name,
        String ownerEmail,
        boolean isPublic,
        boolean banned,
        long cardCount,
        Instant createdAt) {
}
