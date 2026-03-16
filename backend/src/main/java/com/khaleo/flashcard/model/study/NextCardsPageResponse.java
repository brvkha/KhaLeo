package com.khaleo.flashcard.model.study;

import java.util.List;

public record NextCardsPageResponse(
        List<StudyCardSummary> items,
        String nextContinuationToken,
        boolean hasMore) {
}
