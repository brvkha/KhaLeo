package com.khaleo.flashcard.service.activitylog;

import com.khaleo.flashcard.config.observability.NewRelicDeckMediaInstrumentation;
import com.khaleo.flashcard.model.dynamo.RatingGiven;
import com.khaleo.flashcard.model.dynamo.StudyActivityLog;
import com.khaleo.flashcard.repository.dynamo.StudyActivityLogRepository;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class StudyActivityLogPublisher {

    private final StudyActivityLogRepository repository;
    private final ActivityLogRetryService retryService;
    private final NewRelicDeckMediaInstrumentation instrumentation;

    public StudyActivityLogPublisher(
            StudyActivityLogRepository repository,
            ActivityLogRetryService retryService,
            @Autowired(required = false) NewRelicDeckMediaInstrumentation instrumentation) {
        this.repository = repository;
        this.retryService = retryService;
        this.instrumentation = instrumentation;
    }

    public CompletableFuture<Void> publishLearningStateEvent(
            UUID userId,
            UUID cardId,
            RatingGiven ratingGiven,
            Long timeSpentMs) {

        StudyActivityLog logEntry = StudyActivityLog.of(
                userId,
                cardId,
                ratingGiven == null ? RatingGiven.GOOD : ratingGiven,
                timeSpentMs == null ? 0L : timeSpentMs);

        return publishAsync(logEntry);
    }

    public CompletableFuture<Void> publishAsync(StudyActivityLog logEntry) {
        return CompletableFuture.runAsync(() -> {
                retryService.publishWithRetry(logEntry, () -> repository.save(logEntry));
                recordOutcome("success", Map.of(
                        "logId", logEntry.getLogId(),
                        "userId", logEntry.getUserId(),
                        "cardId", logEntry.getCardId()));
            })
                .exceptionally(ex -> {
                    log.error("event=activity_log_publish_async_error logId={} reason={}",
                            logEntry.getLogId(), ex.getMessage(), ex);
                    recordOutcome("failure", Map.of(
                            "logId", logEntry.getLogId(),
                            "reason", ex.getClass().getSimpleName()));
                    return null;
                });
    }

    public CompletableFuture<Void> publishRatingEvent(
            UUID userId,
            UUID cardId,
            UUID deckId,
            RatingGiven ratingGiven,
            Long timeSpentMs,
            Integer newInterval,
            BigDecimal newEaseFactor) {

        StudyActivityLog logEntry = StudyActivityLog.of(
            userId,
            cardId,
            deckId,
            ratingGiven == null ? RatingGiven.GOOD : ratingGiven,
            timeSpentMs == null ? 0L : timeSpentMs,
            newInterval,
            newEaseFactor);

        return publishAsync(logEntry);
    }

    private void recordOutcome(String outcome, Map<String, Object> payload) {
        if (instrumentation != null) {
            instrumentation.recordStudyActivityLogOutcome(outcome, payload);
        }
    }
}
