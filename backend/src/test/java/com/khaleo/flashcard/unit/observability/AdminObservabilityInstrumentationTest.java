package com.khaleo.flashcard.unit.observability;

import static org.assertj.core.api.Assertions.assertThat;

import com.khaleo.flashcard.config.observability.NewRelicDeckMediaInstrumentation;
import com.khaleo.flashcard.config.observability.NewRelicPersistenceInstrumentation;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@ExtendWith(OutputCaptureExtension.class)
class AdminObservabilityInstrumentationTest {

    private final NewRelicPersistenceInstrumentation persistenceInstrumentation =
            new NewRelicPersistenceInstrumentation();

    private final NewRelicDeckMediaInstrumentation deckMediaInstrumentation =
            new NewRelicDeckMediaInstrumentation();

    @Test
    void shouldLogDeploymentAndAdminAuthSignals(CapturedOutput output) {
        persistenceInstrumentation.recordDeploymentDispatch("cmd-123", "i-abc", "in-progress");
        persistenceInstrumentation.recordDeploymentResult("cmd-123", "i-abc", "success");
        persistenceInstrumentation.recordAdminAuthorizationDenied("/api/v1/admin/stats", "ROLE_USER");

        assertThat(output.getOut()).contains("deployment_dispatch");
        assertThat(output.getOut()).contains("deployment_result");
        assertThat(output.getOut()).contains("admin_authorization_denied");
    }

    @Test
    void shouldLogAdminStructuredEventNames(CapturedOutput output) {
        deckMediaInstrumentation.recordAdminObservabilityEvent("moderation_success", Map.of("action", "USER_BAN"));

        assertThat(output.getOut()).contains("admin_observability_event");
        assertThat(output.getOut()).contains("moderation_success");
    }
}
