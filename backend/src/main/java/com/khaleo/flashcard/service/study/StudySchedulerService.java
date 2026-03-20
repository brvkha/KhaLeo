package com.khaleo.flashcard.service.study;

import com.khaleo.flashcard.entity.CardLearningState;
import com.khaleo.flashcard.model.dynamo.RatingGiven;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StudySchedulerService {

    private final StudyTimingPolicy studyTimingPolicy;
    private final SpacedRepetitionService spacedRepetitionService;

    public SpacedRepetitionService.RatingOutcome apply(CardLearningState currentState, RatingGiven rating, Instant now) {
        SpacedRepetitionService.RatingOutcome newCardOutcome =
                studyTimingPolicy.applyNewCardTiming(currentState, rating, now);
        if (newCardOutcome != null) {
            return newCardOutcome;
        }
        return spacedRepetitionService.apply(currentState, rating, now);
    }
}
