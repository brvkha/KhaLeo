package com.khaleo.flashcard.config.observability;

import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class NewRelicDeckMediaInstrumentation {

    public void recordDeckOutcome(String operation, String status, Map<String, Object> attributes) {
        log.info("event=new_relic_deck_media metric=deck_outcome operation={} status={} attributes={} count=1",
                operation,
                status,
                attributes);
    }

    public void recordDeckFailure(String operation, String reason, Map<String, Object> attributes) {
        log.warn("event=new_relic_deck_media metric=deck_failure operation={} reason={} attributes={} count=1",
                operation,
                reason,
                attributes);
    }

    public void recordMediaAuthOutcome(String status, Map<String, Object> attributes) {
        log.info("event=new_relic_deck_media metric=media_auth status={} attributes={} count=1", status, attributes);
    }

    public void recordCardOutcome(String operation, String status, Map<String, Object> attributes) {
        log.info("event=new_relic_deck_media metric=card_outcome operation={} status={} attributes={} count=1",
                operation,
                status,
                attributes);
    }

    public void recordCardFailure(String operation, String reason, Map<String, Object> attributes) {
        log.warn("event=new_relic_deck_media metric=card_failure operation={} reason={} attributes={} count=1",
                operation,
                reason,
                attributes);
    }

    public void recordCardSearch(String status, Map<String, Object> attributes) {
        log.info("event=new_relic_deck_media metric=card_search status={} attributes={} count=1", status, attributes);
    }

    public void recordMediaCleanup(String status, Map<String, Object> attributes) {
        log.info("event=new_relic_deck_media metric=media_cleanup status={} attributes={} count=1", status, attributes);
    }
}
