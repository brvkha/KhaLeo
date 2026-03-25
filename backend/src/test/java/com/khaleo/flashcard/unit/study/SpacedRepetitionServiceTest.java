package com.khaleo.flashcard.unit.study;

import static org.assertj.core.api.Assertions.assertThat;

import com.khaleo.flashcard.entity.CardLearningState;
import com.khaleo.flashcard.entity.enums.CardLearningStateType;
import com.khaleo.flashcard.model.dynamo.RatingGiven;
import com.khaleo.flashcard.service.study.SpacedRepetitionService;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class SpacedRepetitionServiceTest {

    private final SpacedRepetitionService service = new SpacedRepetitionService();

    @Test
    void shouldInitializeFsrsForNewCardOnGood() {
        CardLearningState state = CardLearningState.builder()
                .state(CardLearningStateType.NEW)
                .build();

        Instant now = Instant.now();
        SpacedRepetitionService.RatingOutcome outcome = service.apply(state, RatingGiven.GOOD, now);

        assertThat(outcome.state()).isEqualTo(CardLearningStateType.LEARNING);
        assertThat(outcome.scheduledDays()).isZero();
        assertThat(Duration.between(now, outcome.nextReviewAt()).toMinutes()).isBetween(10L, 11L);
        assertThat(outcome.stability()).isPositive();
        assertThat(outcome.difficulty()).isBetween(BigDecimal.ONE, BigDecimal.TEN);
    }

    @Test
    void shouldGraduateLearningCardToReviewOnSecondGood() {
        CardLearningState state = CardLearningState.builder()
                .state(CardLearningStateType.LEARNING)
                .learningStepGoodCount(1)
                .fsrsDifficulty(BigDecimal.valueOf(5.5))
                .fsrsStability(BigDecimal.valueOf(2.5))
                .lastReviewedAt(Instant.now().minusSeconds(24L * 60L * 60L))
                .build();

        Instant now = Instant.now();
        SpacedRepetitionService.RatingOutcome outcome = service.apply(state, RatingGiven.GOOD, now);

        assertThat(outcome.state()).isEqualTo(CardLearningStateType.REVIEW);
        assertThat(outcome.scheduledDays()).isGreaterThanOrEqualTo(1);
        assertThat(Duration.between(now, outcome.nextReviewAt()).toDays()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void shouldMoveReviewCardToRelearningOnAgain() {
        CardLearningState state = CardLearningState.builder()
                .state(CardLearningStateType.REVIEW)
                .fsrsDifficulty(BigDecimal.valueOf(5.5))
                .fsrsStability(BigDecimal.valueOf(3.2))
                .lastReviewedAt(Instant.now().minusSeconds(3L * 24L * 60L * 60L))
                .fsrsLapses(0)
                .build();

        Instant now = Instant.now();
        SpacedRepetitionService.RatingOutcome outcome = service.apply(state, RatingGiven.AGAIN, now);

        assertThat(outcome.state()).isEqualTo(CardLearningStateType.RELEARNING);
        assertThat(outcome.scheduledDays()).isZero();
        assertThat(Duration.between(now, outcome.nextReviewAt()).toSeconds()).isBetween(60L, 61L);
        assertThat(outcome.lapses()).isEqualTo(1);
        assertThat(outcome.stability()).isPositive();
    }

    @Test
    void shouldIncreaseRepsOnEveryRating() {
        CardLearningState state = CardLearningState.builder()
                .state(CardLearningStateType.REVIEW)
                .fsrsDifficulty(BigDecimal.valueOf(5.5))
                .fsrsStability(BigDecimal.valueOf(2.5))
                .fsrsReps(4)
                .lastReviewedAt(Instant.now().minusSeconds(24L * 60L * 60L))
                .build();

        SpacedRepetitionService.RatingOutcome outcome = service.apply(state, RatingGiven.HARD, Instant.now());

        assertThat(outcome.reps()).isEqualTo(5);
        assertThat(outcome.elapsedDays()).isGreaterThanOrEqualTo(1);
    }
}
