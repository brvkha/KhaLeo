package com.khaleo.flashcard.service.study;

import com.khaleo.flashcard.entity.CardLearningState;
import com.khaleo.flashcard.entity.enums.CardLearningStateType;
import com.khaleo.flashcard.model.dynamo.RatingGiven;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SpacedRepetitionService {

        private static final double[] DEFAULT_WEIGHTS = {
            1.2682, 1.2682, 6.4994, 16.1563,
            6.9135, 0.6470, 2.5935, 0.0010,
            1.7036, 0.1711, 1.1668, 2.0287,
            0.0767, 0.4215, 2.5117, 0.2713,
            3.6253, 0.4372, 0.0468
        };
        private static final double REQUEST_RETENTION = 0.9;
    private static final long MAX_INTERVAL = 36500;
    private static final long AGAIN_REVIEW_DELAY_MINUTES = 1;
    private static final long HARD_LEARNING_DELAY_MINUTES = 5;
    private static final long GOOD_LEARNING_DELAY_MINUTES = 10;

    private final StudyAlgorithmSettingsService studyAlgorithmSettingsService;

    private volatile double[] weights = DEFAULT_WEIGHTS.clone();

    @Autowired
    public SpacedRepetitionService(StudyAlgorithmSettingsService studyAlgorithmSettingsService) {
        this.studyAlgorithmSettingsService = studyAlgorithmSettingsService;
    }

    // Test-only constructor for unit tests that instantiate this service directly.
    public SpacedRepetitionService() {
        this.studyAlgorithmSettingsService = null;
    }

    @PostConstruct
    void initializeWeights() {
        if (studyAlgorithmSettingsService == null) {
            return;
        }
        weights = studyAlgorithmSettingsService.loadOrDefault(DEFAULT_WEIGHTS);
    }

    public RatingOutcome apply(CardLearningState currentState, RatingGiven rating, Instant now) {
        double[] w = weights;
        CardLearningStateType state = normalizeState(currentState.getState());
        double elapsedDays = calculateElapsedDays(currentState, now, state);  // Changed from int to double
        int reps = defaultZero(currentState.getFsrsReps()) + 1;
        int lapses = defaultZero(currentState.getFsrsLapses());
        int learningStepGoodCount = defaultZero(currentState.getLearningStepGoodCount());

        double currentStability = safePositive(currentState.getFsrsStability(), 0.0);
        double currentDifficulty = safePositive(currentState.getFsrsDifficulty(), 0.0);

        CardLearningStateType nextState;
        double nextDifficulty;
        double nextStability;

        if (state == CardLearningStateType.NEW || currentDifficulty <= 0 || currentStability <= 0) {
            nextDifficulty = initDifficulty(rating, w);
            nextStability = initStability(rating, w);
            nextState = rating == RatingGiven.EASY ? CardLearningStateType.REVIEW : CardLearningStateType.LEARNING;
        } else if (state == CardLearningStateType.REVIEW) {
            double retrievability = calculateRetrievability(elapsedDays, currentStability);
            nextDifficulty = nextDifficulty(currentDifficulty, rating, w);
            if (rating == RatingGiven.AGAIN) {
                lapses += 1;
                nextStability = forgetStability(nextDifficulty, currentStability, retrievability, w);
                nextState = CardLearningStateType.RELEARNING;
            } else {
                nextStability = recallStability(nextDifficulty, currentStability, retrievability, rating, w);
                nextState = CardLearningStateType.REVIEW;
            }
        } else {
            // For learning/relearning, derive retrievability from real elapsed time to avoid unrealistic jumps.
            double learningRetrievability = Math.min(
                    0.99,
                    calculateRetrievability(elapsedDays, Math.max(currentStability, 0.1)));
            nextDifficulty = nextDifficulty(currentDifficulty, rating, w);
            if (rating == RatingGiven.AGAIN) {
                nextStability = Math.max(0.1, initStability(RatingGiven.AGAIN, w));
                nextState = CardLearningStateType.RELEARNING;
            } else if (rating == RatingGiven.HARD) {
                nextStability = sameDayStability(currentStability, rating, w);
                nextState = state;
            } else if (rating == RatingGiven.GOOD) {
                if (state == CardLearningStateType.LEARNING || learningStepGoodCount >= 1) {
                    nextStability = sameDayStability(currentStability, rating, w);
                    nextState = CardLearningStateType.REVIEW;
                } else {
                    nextStability = sameDayStability(currentStability, rating, w);
                    nextState = state;
                }
            } else {
                nextStability = sameDayStability(currentStability, rating, w);
                nextState = CardLearningStateType.REVIEW;
            }
        }

        boolean scheduleShortAgain = rating == RatingGiven.AGAIN;
        boolean scheduleShortHard = rating == RatingGiven.HARD
                && (nextState == CardLearningStateType.LEARNING || nextState == CardLearningStateType.RELEARNING);
        boolean scheduleShortGood = rating == RatingGiven.GOOD
                && (nextState == CardLearningStateType.LEARNING || nextState == CardLearningStateType.RELEARNING);

        long scheduledDays;
        Instant nextReviewAt;
        if (scheduleShortAgain) {
            scheduledDays = 0;
            nextReviewAt = now.plus(AGAIN_REVIEW_DELAY_MINUTES, ChronoUnit.MINUTES);
        } else if (scheduleShortHard) {
            scheduledDays = 0;
            nextReviewAt = now.plus(HARD_LEARNING_DELAY_MINUTES, ChronoUnit.MINUTES);
        } else if (scheduleShortGood) {
            scheduledDays = 0;
            nextReviewAt = now.plus(GOOD_LEARNING_DELAY_MINUTES, ChronoUnit.MINUTES);
        } else {
            scheduledDays = calculateInterval(nextStability);
            nextReviewAt = now.plus(scheduledDays, ChronoUnit.DAYS);
        }

        return new RatingOutcome(
                nextState,
            nextReviewAt,
                (int) scheduledDays,
                BigDecimal.valueOf(nextStability),
                BigDecimal.valueOf(nextDifficulty),
                (int) elapsedDays,  // Cast double to int for output
                reps,
                lapses,
                now);
    }

    public int weightCount() {
        return DEFAULT_WEIGHTS.length;
    }

    public double[] getWeights() {
        return weights.clone();
    }

    public synchronized double[] updateWeights(double[] nextWeights) {
        validateWeights(nextWeights);
        weights = nextWeights.clone();
        if (studyAlgorithmSettingsService != null) {
            studyAlgorithmSettingsService.save(weights);
        }
        return getWeights();
    }

    public synchronized double[] resetWeights() {
        weights = DEFAULT_WEIGHTS.clone();
        if (studyAlgorithmSettingsService != null) {
            studyAlgorithmSettingsService.save(weights);
        }
        return getWeights();
    }

    private void validateWeights(double[] candidate) {
        if (candidate == null || candidate.length != DEFAULT_WEIGHTS.length) {
            throw new IllegalArgumentException("weights must contain exactly " + DEFAULT_WEIGHTS.length + " values");
        }
        for (int i = 0; i < candidate.length; i++) {
            if (!Double.isFinite(candidate[i])) {
                throw new IllegalArgumentException("weight at index " + i + " must be finite");
            }
        }
    }

    private CardLearningStateType normalizeState(CardLearningStateType state) {
        if (state == null) {
            return CardLearningStateType.NEW;
        }
        return state == CardLearningStateType.MASTERED ? CardLearningStateType.REVIEW : state;
    }

    private double calculateElapsedDays(CardLearningState state, Instant now, CardLearningStateType normalizedState) {
        if (normalizedState == CardLearningStateType.NEW || state.getLastReviewedAt() == null) {
            return 0.0;
        }
        long secondsBetween = ChronoUnit.SECONDS.between(state.getLastReviewedAt(), now);
        return Math.max(0.0, secondsBetween / 86400.0);  // Convert seconds to fractional days
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

    private double initStability(RatingGiven rating, double[] w) {
        return w[ratingToIndex(rating)];
    }

    private double initDifficulty(RatingGiven rating, double[] w) {
        double d = w[4] - w[5] * (ratingToValue(rating) - 3);
        return constrainDifficulty(d);
    }

    private double nextDifficulty(double currentDifficulty, RatingGiven rating, double[] w) {
        double next = currentDifficulty - w[6] * (ratingToValue(rating) - 3);
        return constrainDifficulty(next);
    }

    private double constrainDifficulty(double value) {
        return Math.min(Math.max(value, 1.0), 10.0);
    }

    private double calculateRetrievability(double elapsedDays, double stability) {
        double safeStability = Math.max(stability, 0.1);
        return Math.pow(1 + (elapsedDays / (9.0 * safeStability)), -1);  // elapsedDays is already double
    }

    private double recallStability(double difficulty, double stability, double retrievability, RatingGiven rating, double[] w) {
        double safeStability = Math.max(stability, 0.1);
        double hardPenalty = rating == RatingGiven.HARD ? w[15] : 1.0;
        double easyBonus = rating == RatingGiven.EASY ? w[16] : 1.0;
        double next = safeStability * (1
                + Math.exp(w[8])
                * (11 - difficulty)
                * Math.pow(safeStability, -w[9])
                * (Math.exp(w[10] * (1 - retrievability)) - 1)
                * hardPenalty
                * easyBonus);
        return Math.max(next, 0.1);
    }

    private double forgetStability(double difficulty, double stability, double retrievability, double[] w) {
        double safeStability = Math.max(stability, 0.1);
        double next = w[11]
                * Math.pow(difficulty, -w[12])
                * (Math.pow(safeStability + 1, w[13]) - 1)
                * Math.exp(w[14] * (1 - retrievability));
        return Math.max(next, 0.1);
    }

    private double sameDayStability(double stability, RatingGiven rating, double[] w) {
        double safeStability = Math.max(stability, 0.1);
        double exponent = w[17] * (ratingToValue(rating) - 3 + w[18]);
        return Math.max(0.1, safeStability * Math.exp(exponent));
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
