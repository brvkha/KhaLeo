package com.khaleo.flashcard.unit;

import static org.assertj.core.api.Assertions.assertThat;

import com.khaleo.flashcard.entity.CardLearningState;
import com.khaleo.flashcard.entity.enums.CardLearningStateType;
import com.khaleo.flashcard.model.dynamo.RatingGiven;
import com.khaleo.flashcard.service.study.StudyTimingPolicy;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class StudyTimingMappingUnitTest {

    private final StudyTimingPolicy policy = new StudyTimingPolicy();

    @Test
    void shouldMapNewCardAgainToOneMinute() {
        CardLearningState state = CardLearningState.builder().state(CardLearningStateType.NEW).build();
        Instant now = Instant.now();
        var outcome = policy.applyNewCardTiming(state, RatingGiven.AGAIN, now);
        assertThat(Duration.between(now, outcome.nextReviewAt()).toSeconds()).isEqualTo(60);
    }

    @Test
    void shouldMapNewCardHardToSixMinutes() {
        CardLearningState state = CardLearningState.builder().state(CardLearningStateType.NEW).build();
        Instant now = Instant.now();
        var outcome = policy.applyNewCardTiming(state, RatingGiven.HARD, now);
        assertThat(Duration.between(now, outcome.nextReviewAt()).toSeconds()).isEqualTo(6 * 60L);
    }

    @Test
    void shouldMapNewCardGoodToTenMinutes() {
        CardLearningState state = CardLearningState.builder().state(CardLearningStateType.NEW).build();
        Instant now = Instant.now();
        var outcome = policy.applyNewCardTiming(state, RatingGiven.GOOD, now);
        assertThat(Duration.between(now, outcome.nextReviewAt()).toSeconds()).isEqualTo(10 * 60L);
    }

    @Test
    void shouldMapNewCardEasyToOneDay() {
        CardLearningState state = CardLearningState.builder().state(CardLearningStateType.NEW).build();
        Instant now = Instant.now();
        var outcome = policy.applyNewCardTiming(state, RatingGiven.EASY, now);
        assertThat(Duration.between(now, outcome.nextReviewAt()).toSeconds()).isEqualTo(24 * 60 * 60L);
    }
}
