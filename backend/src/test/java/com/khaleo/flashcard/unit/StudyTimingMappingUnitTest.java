package com.khaleo.flashcard.unit;

import static org.assertj.core.api.Assertions.assertThat;

import com.khaleo.flashcard.entity.CardLearningState;
import com.khaleo.flashcard.entity.enums.CardLearningStateType;
import com.khaleo.flashcard.model.dynamo.RatingGiven;
import com.khaleo.flashcard.service.study.SpacedRepetitionService;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class StudyTimingMappingUnitTest {

    private final SpacedRepetitionService policy = new SpacedRepetitionService();

    @Test
    void shouldMapNewCardAgainToLearningState() {
        CardLearningState state = CardLearningState.builder().state(CardLearningStateType.NEW).build();
        Instant now = Instant.now();
        var outcome = policy.apply(state, RatingGiven.AGAIN, now);
        assertThat(outcome.state()).isEqualTo(CardLearningStateType.LEARNING);
        assertThat(outcome.scheduledDays()).isZero();
        assertThat(Duration.between(now, outcome.nextReviewAt()).toSeconds()).isBetween(60L, 61L);
    }

    @Test
    void shouldMapNewCardHardToLearningState() {
        CardLearningState state = CardLearningState.builder().state(CardLearningStateType.NEW).build();
        Instant now = Instant.now();
        var outcome = policy.apply(state, RatingGiven.HARD, now);
        assertThat(outcome.state()).isEqualTo(CardLearningStateType.LEARNING);
        assertThat(Duration.between(now, outcome.nextReviewAt()).toDays()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void shouldMapNewCardGoodToLearningState() {
        CardLearningState state = CardLearningState.builder().state(CardLearningStateType.NEW).build();
        Instant now = Instant.now();
        var outcome = policy.apply(state, RatingGiven.GOOD, now);
        assertThat(outcome.state()).isEqualTo(CardLearningStateType.LEARNING);
        assertThat(Duration.between(now, outcome.nextReviewAt()).toDays()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void shouldMapNewCardEasyToReviewState() {
        CardLearningState state = CardLearningState.builder().state(CardLearningStateType.NEW).build();
        Instant now = Instant.now();
        var outcome = policy.apply(state, RatingGiven.EASY, now);
        assertThat(outcome.state()).isEqualTo(CardLearningStateType.REVIEW);
        assertThat(Duration.between(now, outcome.nextReviewAt()).toDays()).isGreaterThanOrEqualTo(1);
    }
}
