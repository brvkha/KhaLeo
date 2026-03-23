package com.khaleo.flashcard.unit.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.khaleo.flashcard.entity.CardLearningState;
import com.khaleo.flashcard.entity.enums.CardLearningStateType;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class CardLearningStateDefaultsTest {

    @Test
    void shouldApplyDefaultValuesWhenUnset() throws Exception {
        CardLearningState state = CardLearningState.builder()
                .state(null)
                .easeFactor(null)
                .intervalInDays(null)
            .fsrsStability(null)
            .fsrsDifficulty(null)
            .fsrsElapsedDays(null)
            .fsrsScheduledDays(null)
            .fsrsReps(null)
            .fsrsLapses(null)
                .build();

        Method defaultsMethod = CardLearningState.class.getDeclaredMethod("applyDefaults");
        defaultsMethod.setAccessible(true);
        defaultsMethod.invoke(state);

        assertThat(state.getState()).isEqualTo(CardLearningStateType.NEW);
        assertThat(state.getEaseFactor()).isEqualByComparingTo(BigDecimal.valueOf(2.5));
        assertThat(state.getIntervalInDays()).isZero();
        assertThat(state.getFsrsStability()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(state.getFsrsDifficulty()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(state.getFsrsElapsedDays()).isZero();
        assertThat(state.getFsrsScheduledDays()).isZero();
        assertThat(state.getFsrsReps()).isZero();
        assertThat(state.getFsrsLapses()).isZero();
    }
}
