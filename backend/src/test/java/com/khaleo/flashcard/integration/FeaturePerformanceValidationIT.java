package com.khaleo.flashcard.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.khaleo.flashcard.entity.CardLearningState;
import com.khaleo.flashcard.entity.enums.CardLearningStateType;
import com.khaleo.flashcard.model.dynamo.RatingGiven;
import com.khaleo.flashcard.service.study.StudyTimingPolicy;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class FeaturePerformanceValidationIT {

    private final StudyTimingPolicy studyTimingPolicy = new StudyTimingPolicy();

    @Test
    void shouldMeetSc003SchedulerMappingLatency() {
        CardLearningState state = CardLearningState.builder()
                .state(CardLearningStateType.NEW)
                .easeFactor(BigDecimal.valueOf(2.5))
                .build();

        Instant started = Instant.now();
        for (int i = 0; i < 5_000; i++) {
            studyTimingPolicy.applyNewCardTiming(state, RatingGiven.GOOD, Instant.now());
        }
        long elapsedMs = Duration.between(started, Instant.now()).toMillis();

        assertThat(elapsedMs).isLessThan(250L);
    }

    @Test
    void shouldMeetSc006RatingDecisionLatency() {
        CardLearningState state = CardLearningState.builder()
                .state(CardLearningStateType.NEW)
                .easeFactor(BigDecimal.valueOf(2.5))
                .build();

        Instant started = Instant.now();
        for (int i = 0; i < 5_000; i++) {
            studyTimingPolicy.applyNewCardTiming(state, RatingGiven.AGAIN, Instant.now());
            studyTimingPolicy.applyNewCardTiming(state, RatingGiven.HARD, Instant.now());
            studyTimingPolicy.applyNewCardTiming(state, RatingGiven.GOOD, Instant.now());
            studyTimingPolicy.applyNewCardTiming(state, RatingGiven.EASY, Instant.now());
        }
        long elapsedMs = Duration.between(started, Instant.now()).toMillis();

        assertThat(elapsedMs).isLessThan(350L);
    }
}
