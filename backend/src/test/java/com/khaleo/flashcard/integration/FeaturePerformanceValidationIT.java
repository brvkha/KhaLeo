package com.khaleo.flashcard.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.khaleo.flashcard.entity.CardLearningState;
import com.khaleo.flashcard.entity.enums.CardLearningStateType;
import com.khaleo.flashcard.model.dynamo.RatingGiven;
import com.khaleo.flashcard.service.study.SpacedRepetitionService;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class FeaturePerformanceValidationIT {

    private final SpacedRepetitionService spacedRepetitionService = new SpacedRepetitionService();

    @Test
    void shouldMeetSc003SchedulerMappingLatency() {
        CardLearningState state = CardLearningState.builder()
                .state(CardLearningStateType.NEW)
                .easeFactor(BigDecimal.valueOf(2.5))
                .build();

        Instant started = Instant.now();
        for (int i = 0; i < 5_000; i++) {
            spacedRepetitionService.apply(state, RatingGiven.GOOD, Instant.now());
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
            spacedRepetitionService.apply(state, RatingGiven.AGAIN, Instant.now());
            spacedRepetitionService.apply(state, RatingGiven.HARD, Instant.now());
            spacedRepetitionService.apply(state, RatingGiven.GOOD, Instant.now());
            spacedRepetitionService.apply(state, RatingGiven.EASY, Instant.now());
        }
        long elapsedMs = Duration.between(started, Instant.now()).toMillis();

        assertThat(elapsedMs).isLessThan(350L);
    }
}
