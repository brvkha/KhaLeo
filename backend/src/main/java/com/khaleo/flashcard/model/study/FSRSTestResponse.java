package com.khaleo.flashcard.model.study;

import java.time.Instant;

/**
 * FSRS algorithm test result - shows predicted next review time for each rating.
 */
public record FSRSTestResponse(
        FSRSRatingResult againResult,
        FSRSRatingResult hardResult,
        FSRSRatingResult goodResult,
        FSRSRatingResult easyResult) {

    public record FSRSRatingResult(
            String rating,
            Instant nextReviewAt,
            Integer scheduledDays,
            Double stability,
            Double difficulty,
            Integer reps,
            Integer lapses,
            String nextState,
            Integer learningStepGoodCount) {
    }
}
