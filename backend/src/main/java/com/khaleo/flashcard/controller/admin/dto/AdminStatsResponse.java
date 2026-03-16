package com.khaleo.flashcard.controller.admin.dto;

import java.time.Instant;

public record AdminStatsResponse(
        long totalUsers,
        long totalDecks,
        long totalCards,
        long reviewsLast24Hours,
        Instant generatedAt) {
}
