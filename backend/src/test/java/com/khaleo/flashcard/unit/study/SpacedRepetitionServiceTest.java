package com.khaleo.flashcard.unit.study;

import static org.assertj.core.api.Assertions.assertThat;

import com.khaleo.flashcard.entity.CardLearningState;
import com.khaleo.flashcard.entity.enums.CardLearningStateType;
import com.khaleo.flashcard.model.dynamo.RatingGiven;
import com.khaleo.flashcard.service.study.SpacedRepetitionService;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class SpacedRepetitionServiceTest {

    private final SpacedRepetitionService service = new SpacedRepetitionService();

    @Test
    void shouldMoveNewCardToLearningOnFirstGood() {
        CardLearningState state = CardLearningState.builder()
                .state(CardLearningStateType.NEW)
                .easeFactor(BigDecimal.valueOf(2.5))
                .intervalInDays(0)
                .learningStepGoodCount(0)
                .build();

        SpacedRepetitionService.RatingOutcome outcome = service.apply(state, RatingGiven.GOOD, Instant.now());

        assertThat(outcome.state()).isEqualTo(CardLearningStateType.LEARNING);
        assertThat(outcome.intervalInDays()).isZero();
        assertThat(outcome.learningStepGoodCount()).isEqualTo(1);
    }

    @Test
    void shouldMoveLearningCardToMasteredOnSecondGood() {
        CardLearningState state = CardLearningState.builder()
                .state(CardLearningStateType.LEARNING)
                .easeFactor(BigDecimal.valueOf(2.5))
                .intervalInDays(0)
                .learningStepGoodCount(1)
                .build();

        SpacedRepetitionService.RatingOutcome outcome = service.apply(state, RatingGiven.GOOD, Instant.now());

        assertThat(outcome.state()).isEqualTo(CardLearningStateType.MASTERED);
        assertThat(outcome.intervalInDays()).isEqualTo(1);
    }

    @Test
    void shouldClampEaseFactorAtMinimumOnAgain() {
        CardLearningState state = CardLearningState.builder()
                .state(CardLearningStateType.REVIEW)
                .easeFactor(BigDecimal.valueOf(1.35))
                .intervalInDays(5)
                .learningStepGoodCount(1)
                .build();

        SpacedRepetitionService.RatingOutcome outcome = service.apply(state, RatingGiven.AGAIN, Instant.now());

        assertThat(outcome.state()).isEqualTo(CardLearningStateType.LEARNING);
        assertThat(outcome.intervalInDays()).isZero();
        assertThat(outcome.easeFactor()).isEqualByComparingTo("1.3");
    }
}
