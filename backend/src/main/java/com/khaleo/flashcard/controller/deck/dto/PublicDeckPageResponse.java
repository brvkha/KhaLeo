package com.khaleo.flashcard.controller.deck.dto;

import java.util.List;

public record PublicDeckPageResponse(
        List<PublicDeckSummaryResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages) {
}
