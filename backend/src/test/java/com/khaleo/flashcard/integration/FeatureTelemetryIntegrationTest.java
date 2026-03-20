package com.khaleo.flashcard.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.khaleo.flashcard.config.FeatureTelemetryLogger;
import com.khaleo.flashcard.config.observability.NewRelicDeckMediaInstrumentation;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@ExtendWith(OutputCaptureExtension.class)
class FeatureTelemetryIntegrationTest {

    private final FeatureTelemetryLogger telemetryLogger = new FeatureTelemetryLogger();
    private final NewRelicDeckMediaInstrumentation instrumentation = new NewRelicDeckMediaInstrumentation();

    @Test
    void shouldEmitDiscoveryImportMergeAndSchedulingTelemetry(CapturedOutput output) {
        telemetryLogger.info("public_discovery_accessed", Map.of("query", "biology"));
        telemetryLogger.info("public_import_started", Map.of("deckId", "d-1"));
        telemetryLogger.info("reimport_merge_resolved", Map.of("importLinkId", "l-1"));

        instrumentation.recordDiscoveryOutcome("success", Map.of("page", 0));
        instrumentation.recordImportOutcome("success", Map.of("deckId", "d-1"));
        instrumentation.recordMergeOutcome("success", Map.of("importLinkId", "l-1"));
        instrumentation.recordStudyNextCardsOutcome("success", Map.of("deckId", "d-1"));
        instrumentation.recordStudyRatingOutcome("success", Map.of("cardId", "c-1"));

        assertThat(output.getOut()).contains("public_discovery_accessed");
        assertThat(output.getOut()).contains("public_import_started");
        assertThat(output.getOut()).contains("reimport_merge_resolved");
        assertThat(output.getOut()).contains("metric=public_discovery");
        assertThat(output.getOut()).contains("metric=public_import");
        assertThat(output.getOut()).contains("metric=reimport_merge");
        assertThat(output.getOut()).contains("metric=study_next_cards");
        assertThat(output.getOut()).contains("metric=study_rating");
    }
}
