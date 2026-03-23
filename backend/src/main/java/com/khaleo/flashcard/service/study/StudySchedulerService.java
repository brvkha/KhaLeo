package com.khaleo.flashcard.service.study;

import com.khaleo.flashcard.entity.CardLearningState;
import com.khaleo.flashcard.model.dynamo.RatingGiven;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StudySchedulerService {

    private final SpacedRepetitionService spacedRepetitionService;

    public SpacedRepetitionService.RatingOutcome apply(CardLearningState currentState, RatingGiven rating, Instant now) {
        return spacedRepetitionService.apply(currentState, rating, now);
    }
}
