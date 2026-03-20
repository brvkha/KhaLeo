package com.khaleo.flashcard.service.study;

import com.khaleo.flashcard.entity.CardLearningState;
import com.khaleo.flashcard.entity.enums.CardLearningStateType;
import com.khaleo.flashcard.model.dynamo.RatingGiven;
import java.math.BigDecimal;
import java.time.Instant;
import org.springframework.stereotype.Service;

@Service
public class StudyTimingPolicy {

    public SpacedRepetitionService.RatingOutcome applyNewCardTiming(
            CardLearningState currentState,
            RatingGiven rating,
            Instant now) {
        if (currentState.getState() != CardLearningStateType.NEW) {
            return null;
        }

        return switch (rating) {
            case AGAIN -> new SpacedRepetitionService.RatingOutcome(
                    CardLearningStateType.LEARNING,
                    now.plusSeconds(60),
                    0,
                    safeEase(currentState.getEaseFactor()),
                    0,
                    now);
            case HARD -> new SpacedRepetitionService.RatingOutcome(
                    CardLearningStateType.LEARNING,
                    now.plusSeconds(6 * 60L),
                    0,
                    safeEase(currentState.getEaseFactor()),
                    0,
                    now);
            case GOOD -> new SpacedRepetitionService.RatingOutcome(
                    CardLearningStateType.LEARNING,
                    now.plusSeconds(10 * 60L),
                    0,
                    safeEase(currentState.getEaseFactor()),
                    1,
                    now);
            case EASY -> new SpacedRepetitionService.RatingOutcome(
                    CardLearningStateType.REVIEW,
                    now.plusSeconds(24 * 60 * 60L),
                    1,
                    safeEase(currentState.getEaseFactor()).add(BigDecimal.valueOf(0.15)),
                    1,
                    now);
        };
    }

    private BigDecimal safeEase(BigDecimal ease) {
        return ease == null ? BigDecimal.valueOf(2.5) : ease;
    }
}
