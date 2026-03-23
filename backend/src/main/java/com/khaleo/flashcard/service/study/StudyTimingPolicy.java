package com.khaleo.flashcard.service.study;

import com.khaleo.flashcard.entity.CardLearningState;
import com.khaleo.flashcard.model.dynamo.RatingGiven;
import java.time.Instant;
import org.springframework.stereotype.Service;

@Service
public class StudyTimingPolicy {

    public SpacedRepetitionService.RatingOutcome applyNewCardTiming(
            CardLearningState currentState,
            RatingGiven rating,
            Instant now) {
        return null;
    }
}
