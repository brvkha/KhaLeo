package com.khaleo.flashcard.model.study;

import com.khaleo.flashcard.entity.enums.CardLearningStateType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record RateCardResponse(
        UUID cardId,
        CardLearningStateType state,
        Instant nextReviewAt,
        Integer scheduledDays,
        BigDecimal newStability,
        BigDecimal newDifficulty) {
}
