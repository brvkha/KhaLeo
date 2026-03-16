package com.khaleo.flashcard.config.observability;

import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class NewRelicAuthInstrumentation {

    public void recordAuthOutcome(String event, Map<String, Object> attributes) {
        log.info("event=new_relic_auth_metric metric=auth_outcome authEvent={} attributes={} count=1", event, attributes);
    }

    public void recordAuthFailure(String event, String reason, Map<String, Object> attributes) {
        log.warn(
                "event=new_relic_auth_metric metric=auth_failure authEvent={} reason={} attributes={} count=1",
                event,
                reason,
                attributes);
    }

    public void recordAdminModerationOutcome(String action, String status, Map<String, Object> attributes) {
        log.info(
                "event=new_relic_auth_metric metric=admin_moderation_outcome action={} status={} attributes={} count=1",
                action,
                status,
                attributes);
    }

    public void recordAdminModerationFailure(String action, String reason, Map<String, Object> attributes) {
        log.warn(
                "event=new_relic_auth_metric metric=admin_moderation_failure action={} reason={} attributes={} count=1",
                action,
                reason,
                attributes);
    }
}