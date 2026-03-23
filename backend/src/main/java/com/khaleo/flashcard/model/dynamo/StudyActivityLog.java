package com.khaleo.flashcard.model.dynamo;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudyActivityLog {

    private String logId;
    private String timestamp;
    private String userId;
    private String cardId;
    private String deckId;
    private RatingGiven ratingGiven;
    private Long timeSpentMs;
    private Integer scheduledDays;
    private String newStability;
    private String newDifficulty;
    private String writeStatus;

    public static StudyActivityLog of(UUID userId, UUID cardId, RatingGiven ratingGiven, Long timeSpentMs) {
        return of(userId, cardId, null, ratingGiven, timeSpentMs, null, null, null);
    }

    public static StudyActivityLog of(
            UUID userId,
            UUID cardId,
            UUID deckId,
            RatingGiven ratingGiven,
            Long timeSpentMs,
            Integer scheduledDays,
            java.math.BigDecimal newStability,
            java.math.BigDecimal newDifficulty) {
        return StudyActivityLog.builder()
                .logId(UUID.randomUUID().toString())
                .timestamp(Instant.now().toString())
                .userId(userId.toString())
                .cardId(cardId.toString())
                .deckId(deckId == null ? null : deckId.toString())
                .ratingGiven(ratingGiven)
                .timeSpentMs(timeSpentMs)
                .scheduledDays(scheduledDays)
                .newStability(newStability == null ? null : newStability.toPlainString())
                .newDifficulty(newDifficulty == null ? null : newDifficulty.toPlainString())
                .writeStatus("PENDING")
                .build();
    }

    public Map<String, AttributeValue> toAttributeMap() {
        validate();
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("logId", AttributeValue.fromS(logId));
        item.put("timestamp", AttributeValue.fromS(timestamp));
        item.put("userId", AttributeValue.fromS(userId));
        item.put("cardId", AttributeValue.fromS(cardId));
        if (!isBlank(deckId)) {
            item.put("deckId", AttributeValue.fromS(deckId));
        }
        item.put("ratingGiven", AttributeValue.fromS(ratingGiven.name()));
        item.put("timeSpentMs", AttributeValue.fromN(String.valueOf(timeSpentMs)));
        if (scheduledDays != null) {
            item.put("scheduledDays", AttributeValue.fromN(String.valueOf(scheduledDays)));
        }
        if (!isBlank(newStability)) {
            item.put("newStability", AttributeValue.fromN(newStability));
        }
        if (!isBlank(newDifficulty)) {
            item.put("newDifficulty", AttributeValue.fromN(newDifficulty));
        }
        item.put("writeStatus", AttributeValue.fromS(writeStatus == null ? "PENDING" : writeStatus));
        return item;
    }

    private void validate() {
        if (isBlank(logId) || isBlank(timestamp) || isBlank(userId) || isBlank(cardId)) {
            throw new IllegalStateException("StudyActivityLog requires non-empty logId, timestamp, userId, and cardId.");
        }
        if (ratingGiven == null) {
            throw new IllegalStateException("StudyActivityLog requires ratingGiven.");
        }
        if (timeSpentMs == null || timeSpentMs < 0) {
            throw new IllegalStateException("StudyActivityLog requires non-negative timeSpentMs.");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
