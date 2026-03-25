package com.khaleo.flashcard.model.study;

import java.time.Instant;

public record RateCardPreviewResponse(
        RatingPreview again,
        RatingPreview hard,
        RatingPreview good,
        RatingPreview easy) {

    public record RatingPreview(
            Instant nextReviewAt,
            Integer scheduledDays,
            String nextState) {
    }
}