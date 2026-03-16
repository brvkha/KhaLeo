package com.khaleo.flashcard.service.study;

import com.khaleo.flashcard.entity.CardLearningState;
import com.khaleo.flashcard.entity.enums.CardLearningStateType;
import com.khaleo.flashcard.model.dynamo.RatingGiven;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SpacedRepetitionService {

    private static final BigDecimal MIN_EASE_FACTOR = BigDecimal.valueOf(1.3);

    public RatingOutcome apply(CardLearningState currentState, RatingGiven rating, Instant now) {
        CardLearningStateType state = currentState.getState();
        BigDecimal ease = safeEase(currentState.getEaseFactor());
        int interval = currentState.getIntervalInDays() == null ? 0 : currentState.getIntervalInDays();
        int learningGoodCount = currentState.getLearningStepGoodCount() == null ? 0 : currentState.getLearningStepGoodCount();

        if (rating == RatingGiven.GOOD && state == CardLearningStateType.NEW) {
            return new RatingOutcome(
                    CardLearningStateType.LEARNING,
                    now.plusSeconds(600),
                    0,
                    ease,
                    1,
                    now);
        }

        if (rating == RatingGiven.GOOD && state == CardLearningStateType.LEARNING && learningGoodCount >= 1) {
            return new RatingOutcome(
                    CardLearningStateType.MASTERED,
                    now.plusSeconds(24L * 60L * 60L),
                    1,
                    ease,
                    learningGoodCount + 1,
                    now);
        }

        if (rating == RatingGiven.AGAIN) {
            return new RatingOutcome(
                    CardLearningStateType.LEARNING,
                    now,
                    0,
                    clampMin(ease.subtract(BigDecimal.valueOf(0.2))),
                    0,
                    now);
        }

        CardLearningStateType resolvedState = state == CardLearningStateType.MASTERED ? CardLearningStateType.REVIEW : state;
        BigDecimal nextEase = ease;
        BigDecimal nextInterval;

        switch (rating) {
            case HARD -> {
                nextInterval = BigDecimal.valueOf(interval).multiply(BigDecimal.valueOf(1.2));
                nextEase = clampMin(ease.subtract(BigDecimal.valueOf(0.15)));
            }
            case GOOD -> nextInterval = BigDecimal.valueOf(Math.max(interval, 1)).multiply(ease);
            case EASY -> {
                nextInterval = BigDecimal.valueOf(Math.max(interval, 1))
                        .multiply(ease)
                        .multiply(BigDecimal.valueOf(1.3));
                nextEase = clampMin(ease.add(BigDecimal.valueOf(0.15)));
            }
            default -> throw new IllegalArgumentException("Unsupported rating " + rating);
        }

        int nextIntervalDays = Math.max(1, nextInterval.setScale(0, RoundingMode.HALF_UP).intValue());
        return new RatingOutcome(
                resolvedState,
                now.plusSeconds(nextIntervalDays * 24L * 60L * 60L),
                nextIntervalDays,
                nextEase,
                learningGoodCount,
                now);
    }

    private BigDecimal safeEase(BigDecimal ease) {
        return ease == null ? BigDecimal.valueOf(2.5) : clampMin(ease);
    }

    private BigDecimal clampMin(BigDecimal ease) {
        return ease.compareTo(MIN_EASE_FACTOR) < 0 ? MIN_EASE_FACTOR : ease;
    }

    public record RatingOutcome(
            CardLearningStateType state,
            Instant nextReviewAt,
            Integer intervalInDays,
            BigDecimal easeFactor,
            Integer learningStepGoodCount,
            Instant lastReviewedAt) {
    }
}
