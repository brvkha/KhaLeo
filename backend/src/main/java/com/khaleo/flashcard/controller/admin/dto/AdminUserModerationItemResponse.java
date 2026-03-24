package com.khaleo.flashcard.controller.admin.dto;

import java.time.Instant;
import java.util.UUID;

public record AdminUserModerationItemResponse(
        UUID id,
        String email,
        String role,
        boolean verified,
        boolean banned,
        Instant createdAt) {
}
