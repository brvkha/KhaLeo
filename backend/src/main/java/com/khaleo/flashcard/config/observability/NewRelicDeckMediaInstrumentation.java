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

    public void recordStudyNextCardsOutcome(String status, Map<String, Object> attributes) {
        log.info("event=new_relic_deck_media metric=study_next_cards status={} attributes={} count=1",
                status,
                attributes);
    }

    public void recordStudyNextCardsFailure(String reason, Map<String, Object> attributes) {
        log.warn("event=new_relic_deck_media metric=study_next_cards_failure reason={} attributes={} count=1",
                reason,
                attributes);
    }

    public void recordStudyRatingOutcome(String status, Map<String, Object> attributes) {
        log.info("event=new_relic_deck_media metric=study_rating status={} attributes={} count=1",
                status,
                attributes);
    }

    public void recordStudyRatingFailure(String reason, Map<String, Object> attributes) {
        log.warn("event=new_relic_deck_media metric=study_rating_failure reason={} attributes={} count=1",
                reason,
                attributes);
    }

    public void recordStudyActivityLogOutcome(String status, Map<String, Object> attributes) {
        log.info("event=new_relic_deck_media metric=study_activity_log status={} attributes={} count=1",
                status,
                attributes);
    }

    public void recordDiscoveryOutcome(String status, Map<String, Object> attributes) {
        log.info("event=new_relic_deck_media metric=public_discovery status={} attributes={} count=1",
                status,
                attributes);
    }

    public void recordImportOutcome(String status, Map<String, Object> attributes) {
        log.info("event=new_relic_deck_media metric=public_import status={} attributes={} count=1",
                status,
                attributes);
    }

    public void recordMergeOutcome(String status, Map<String, Object> attributes) {
        log.info("event=new_relic_deck_media metric=reimport_merge status={} attributes={} count=1",
                status,
                attributes);
    }

    public void recordMergeFailure(String reason, Map<String, Object> attributes) {
        log.warn("event=new_relic_deck_media metric=reimport_merge_failure reason={} attributes={} count=1",
                reason,
                attributes);
    }

    public void recordAdminObservabilityEvent(String eventName, Map<String, Object> attributes) {
        log.info("event=new_relic_deck_media metric=admin_observability_event eventName={} attributes={} count=1",
                eventName,
                attributes);
    }
}
