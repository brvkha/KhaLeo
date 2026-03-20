package com.khaleo.flashcard.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.khaleo.flashcard.entity.CardLearningState;
import com.khaleo.flashcard.entity.enums.CardLearningStateType;
import com.khaleo.flashcard.model.dynamo.RatingGiven;
import com.khaleo.flashcard.service.study.StudySchedulerService;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class StudySchedulerLegacyBehaviorIntegrationTest {

    @Autowired
    private StudySchedulerService studySchedulerService;

    @Test
    void shouldPreserveNonNewReviewBehavior() {
        CardLearningState review = CardLearningState.builder()
                .state(CardLearningStateType.REVIEW)
                .intervalInDays(5)
                .easeFactor(BigDecimal.valueOf(2.5))
                .learningStepGoodCount(1)
                .build();

        var outcome = studySchedulerService.apply(review, RatingGiven.GOOD, Instant.now());

        assertThat(outcome.state()).isEqualTo(CardLearningStateType.REVIEW);
        assertThat(outcome.intervalInDays()).isGreaterThanOrEqualTo(1);
        assertThat(outcome.easeFactor()).isGreaterThanOrEqualTo(BigDecimal.valueOf(1.3));
    }
}
