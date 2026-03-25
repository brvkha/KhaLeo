package com.khaleo.flashcard.model.study;

import java.math.BigDecimal;

/**
 * Test FSRS algorithm - returns predicted next review time for each rating.
 * Used in admin testing interface to understand algorithm behavior.
 */
public record FSRSTestRequest(
        BigDecimal stability,
        BigDecimal difficulty,
        Integer reps,
        Integer lapses,
        BigDecimal elapsedDays,
        String state,
        Integer learningStepGoodCount) {
}
