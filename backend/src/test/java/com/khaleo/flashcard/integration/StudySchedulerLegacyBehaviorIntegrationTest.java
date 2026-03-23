package com.khaleo.flashcard.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.khaleo.flashcard.entity.CardLearningState;
import com.khaleo.flashcard.entity.enums.CardLearningStateType;
import com.khaleo.flashcard.model.dynamo.RatingGiven;
import com.khaleo.flashcard.service.study.StudySchedulerService;
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
                .fsrsDifficulty(java.math.BigDecimal.valueOf(5.0))
                .fsrsStability(java.math.BigDecimal.valueOf(4.0))
                .lastReviewedAt(Instant.now().minusSeconds(24L * 60L * 60L))
                .build();

        var outcome = studySchedulerService.apply(review, RatingGiven.GOOD, Instant.now());

        assertThat(outcome.state()).isEqualTo(CardLearningStateType.REVIEW);
        assertThat(outcome.scheduledDays()).isGreaterThanOrEqualTo(1);
        assertThat(outcome.stability()).isPositive();
        assertThat(outcome.difficulty()).isPositive();
    }
}
