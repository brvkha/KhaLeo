package com.khaleo.flashcard.service.auth;

import com.khaleo.flashcard.config.observability.NewRelicAuthInstrumentation;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthAuditLogger {

    private final NewRelicAuthInstrumentation newRelicAuthInstrumentation;

    public void logEvent(String event, Map<String, Object> attributes) {
        log.info("event={} attributes={}", event, attributes);
        newRelicAuthInstrumentation.recordAuthOutcome(event, attributes);
    }
}
