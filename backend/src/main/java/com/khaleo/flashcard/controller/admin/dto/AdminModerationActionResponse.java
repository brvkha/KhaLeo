package com.khaleo.flashcard.controller.admin.dto;

import java.time.Instant;
import java.util.UUID;

public record AdminModerationActionResponse(
        UUID id,
        UUID adminUserId,
        String adminEmail,
        String actionType,
        String targetType,
        UUID targetId,
        String targetDisplayName,
        String status,
        String reasonCode,
        Instant createdAt) {
}
