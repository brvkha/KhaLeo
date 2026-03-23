package com.khaleo.flashcard.service.study;

import com.khaleo.flashcard.entity.CardLearningState;
import com.khaleo.flashcard.entity.enums.CardLearningStateType;
import com.khaleo.flashcard.model.dynamo.RatingGiven;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SpacedRepetitionService {

    private static final double[] W = {
            0.4, 0.6, 2.4, 5.8, 4.93, 0.94, 0.86, 0.01,
            1.49, 0.14, 0.94, 2.18, 0.05, 0.34, 1.26, 0.29, 2.61
    };
    private static final double REQUEST_RETENTION = 0.9;
    private static final long MAX_INTERVAL = 36500;
    private static final long AGAIN_REVIEW_DELAY_MINUTES = 1;

    public RatingOutcome apply(CardLearningState currentState, RatingGiven rating, Instant now) {
        CardLearningStateType state = normalizeState(currentState.getState());
        int elapsedDays = calculateElapsedDays(currentState, now, state);
        int reps = defaultZero(currentState.getFsrsReps()) + 1;
        int lapses = defaultZero(currentState.getFsrsLapses());

        double currentStability = safePositive(currentState.getFsrsStability(), 0.0);
        double currentDifficulty = safePositive(currentState.getFsrsDifficulty(), 0.0);

        CardLearningStateType nextState;
        double nextDifficulty;
        double nextStability;

        if (state == CardLearningStateType.NEW || currentDifficulty <= 0 || currentStability <= 0) {
            nextDifficulty = initDifficulty(rating);
            nextStability = initStability(rating);
            nextState = rating == RatingGiven.EASY ? CardLearningStateType.REVIEW : CardLearningStateType.LEARNING;
        } else if (state == CardLearningStateType.REVIEW) {
            double retrievability = calculateRetrievability(elapsedDays, currentStability);
            nextDifficulty = nextDifficulty(currentDifficulty, rating);
            if (rating == RatingGiven.AGAIN) {
                lapses += 1;
                nextStability = forgetStability(nextDifficulty, currentStability, retrievability);
                nextState = CardLearningStateType.RELEARNING;
            } else {
                nextStability = recallStability(nextDifficulty, currentStability, retrievability, rating);
                nextState = CardLearningStateType.REVIEW;
            }
        } else {
            // Learning/relearning cards can start without stable historical R; use a conservative pseudo-recall.
            double pseudoRetrievability = 0.9;
            nextDifficulty = nextDifficulty(currentDifficulty, rating);
            if (rating == RatingGiven.AGAIN) {
                nextStability = Math.max(0.1, initStability(RatingGiven.AGAIN));
                nextState = CardLearningStateType.RELEARNING;
            } else if (rating == RatingGiven.HARD) {
                nextStability = Math.max(0.1, currentStability * 0.85);
                nextState = state;
            } else {
                nextStability = recallStability(nextDifficulty, currentStability, pseudoRetrievability, rating);
                nextState = CardLearningStateType.REVIEW;
            }
        }

        boolean scheduleShortAgain = rating == RatingGiven.AGAIN;
        long scheduledDays = scheduleShortAgain ? 0 : calculateInterval(nextStability);
        Instant nextReviewAt = scheduleShortAgain
            ? now.plus(AGAIN_REVIEW_DELAY_MINUTES, ChronoUnit.MINUTES)
            : now.plus(scheduledDays, ChronoUnit.DAYS);

        return new RatingOutcome(
                nextState,
            nextReviewAt,
                (int) scheduledDays,
                BigDecimal.valueOf(nextStability),
                BigDecimal.valueOf(nextDifficulty),
                elapsedDays,
                reps,
                lapses,
                now);
    }

    private CardLearningStateType normalizeState(CardLearningStateType state) {
        if (state == null) {
            return CardLearningStateType.NEW;
        }
        return state == CardLearningStateType.MASTERED ? CardLearningStateType.REVIEW : state;
    }

    private int calculateElapsedDays(CardLearningState state, Instant now, CardLearningStateType normalizedState) {
        if (normalizedState == CardLearningStateType.NEW || state.getLastReviewedAt() == null) {
            return 0;
        }
        return (int) Math.max(0, ChronoUnit.DAYS.between(state.getLastReviewedAt(), now));
    }

    private int defaultZero(Integer value) {
        return value == null ? 0 : value;
    }

    private double safePositive(BigDecimal value, double fallback) {
        if (value == null) {
            return fallback;
        }
        return Math.max(fallback, value.doubleValue());
    }

    private double initStability(RatingGiven rating) {
        return W[ratingToIndex(rating)];
    }

    private double initDifficulty(RatingGiven rating) {
        double d = W[4] - W[5] * (ratingToValue(rating) - 3);
        return constrainDifficulty(d);
    }

    private double nextDifficulty(double currentDifficulty, RatingGiven rating) {
        double next = currentDifficulty - W[6] * (ratingToValue(rating) - 3);
        return constrainDifficulty(next);
    }

    private double constrainDifficulty(double value) {
        return Math.min(Math.max(value, 1.0), 10.0);
    }

    private double calculateRetrievability(int elapsedDays, double stability) {
        double safeStability = Math.max(stability, 0.1);
        return Math.pow(1 + ((double) elapsedDays) / (9.0 * safeStability), -1);
    }

    private double recallStability(double difficulty, double stability, double retrievability, RatingGiven rating) {
        double safeStability = Math.max(stability, 0.1);
        double hardPenalty = rating == RatingGiven.HARD ? W[15] : 1.0;
        double easyBonus = rating == RatingGiven.EASY ? W[16] : 1.0;
        double next = safeStability * (1
                + Math.exp(W[8])
                * (11 - difficulty)
                * Math.pow(safeStability, -W[9])
                * (Math.exp(W[10] * (1 - retrievability)) - 1)
                * hardPenalty
                * easyBonus);
        return Math.max(next, 0.1);
    }

    private double forgetStability(double difficulty, double stability, double retrievability) {
        double safeStability = Math.max(stability, 0.1);
        double next = W[11]
                * Math.pow(difficulty, -W[12])
                * (Math.pow(safeStability + 1, W[13]) - 1)
                * Math.exp(W[14] * (1 - retrievability));
        return Math.max(next, 0.1);
    }

    private long calculateInterval(double nextStability) {
        long interval = Math.round(nextStability * 9 * (1 / REQUEST_RETENTION - 1));
        return Math.min(Math.max(interval, 1), MAX_INTERVAL);
    }

    private int ratingToIndex(RatingGiven rating) {
        return ratingToValue(rating) - 1;
    }

    private int ratingToValue(RatingGiven rating) {
        return switch (rating) {
            case AGAIN -> 1;
            case HARD -> 2;
            case GOOD -> 3;
            case EASY -> 4;
        };
    }

    public record RatingOutcome(
            CardLearningStateType state,
            Instant nextReviewAt,
            Integer scheduledDays,
            BigDecimal stability,
            BigDecimal difficulty,
            Integer elapsedDays,
            Integer reps,
            Integer lapses,
            Instant lastReviewedAt) {
    }
}
