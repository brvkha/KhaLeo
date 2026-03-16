package com.khaleo.flashcard.integration.deploy;

import static org.assertj.core.api.Assertions.assertThat;

import com.khaleo.flashcard.service.deploy.DeploymentResultAggregator;
import java.util.List;
import org.junit.jupiter.api.Test;

class DeploymentAggregationIT {

    @Test
    void shouldFailOverallWhenAnyTargetFails() {
        DeploymentResultAggregator.Aggregation aggregation =
                DeploymentResultAggregator.aggregate(List.of("SUCCESS", "FAILED", "SUCCESS"));

        assertThat(aggregation.totalTargets()).isEqualTo(3);
        assertThat(aggregation.succeededTargets()).isEqualTo(2);
        assertThat(aggregation.failedTargets()).isEqualTo(1);
        assertThat(aggregation.successful()).isFalse();
    }

    @Test
    void shouldSucceedWhenAllTargetsSucceed() {
        DeploymentResultAggregator.Aggregation aggregation =
                DeploymentResultAggregator.aggregate(List.of("SUCCESS", "SUCCESS"));

        assertThat(aggregation.totalTargets()).isEqualTo(2);
        assertThat(aggregation.succeededTargets()).isEqualTo(2);
        assertThat(aggregation.failedTargets()).isZero();
        assertThat(aggregation.successful()).isTrue();
    }
}
