package com.khaleo.flashcard.repository.dynamo;

import com.khaleo.flashcard.model.dynamo.RatingGiven;
import com.khaleo.flashcard.model.dynamo.StudyActivityLog;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;

@Repository
@RequiredArgsConstructor
public class DynamoStudyActivityLogRepository implements StudyActivityLogRepository {

    private final DynamoDbClient dynamoDbClient;

    @Value("${app.dynamo.study-activity-log-table:StudyActivityLog}")
    private String tableName;

    @Override
    public void save(StudyActivityLog log) {
        PutItemRequest request = PutItemRequest.builder()
                .tableName(tableName)
                .item(log.toAttributeMap())
                .build();
        dynamoDbClient.putItem(request);
    }

    @Override
    public List<StudyActivityLog> findByUserId(String userId, int limit) {
        QueryRequest request = QueryRequest.builder()
                .tableName(tableName)
                .indexName("userId-timestamp-index")
                .keyConditionExpression("userId = :uid")
                .expressionAttributeValues(Map.of(":uid", AttributeValue.fromS(userId)))
                .scanIndexForward(false)
                .limit(limit)
                .build();

        QueryResponse response = dynamoDbClient.query(request);
        List<StudyActivityLog> logs = new ArrayList<>();
        for (Map<String, AttributeValue> item : response.items()) {
            logs.add(StudyActivityLog.builder()
                    .logId(item.get("logId").s())
                    .timestamp(item.get("timestamp").s())
                    .userId(item.get("userId").s())
                    .cardId(item.get("cardId").s())
                    .deckId(item.containsKey("deckId") ? item.get("deckId").s() : null)
                    .ratingGiven(RatingGiven.valueOf(item.get("ratingGiven").s()))
                    .timeSpentMs(Long.parseLong(item.get("timeSpentMs").n()))
                    .newInterval(item.containsKey("newInterval") ? Integer.parseInt(item.get("newInterval").n()) : null)
                    .newEaseFactor(item.containsKey("newEaseFactor") ? item.get("newEaseFactor").n() : null)
                    .writeStatus(item.containsKey("writeStatus") ? item.get("writeStatus").s() : "UNKNOWN")
                    .build());
        }
        return logs;
    }
}
