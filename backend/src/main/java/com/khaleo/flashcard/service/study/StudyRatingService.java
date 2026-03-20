package com.khaleo.flashcard.service.study;

import com.khaleo.flashcard.config.observability.NewRelicDeckMediaInstrumentation;
import com.khaleo.flashcard.entity.Card;
import com.khaleo.flashcard.entity.CardLearningState;
import com.khaleo.flashcard.model.dynamo.RatingGiven;
import com.khaleo.flashcard.model.study.RateCardRequest;
import com.khaleo.flashcard.model.study.RateCardResponse;
import com.khaleo.flashcard.repository.CardLearningStateRepository;
import com.khaleo.flashcard.repository.UserRepository;
import com.khaleo.flashcard.service.activitylog.StudyActivityLogPublisher;
import com.khaleo.flashcard.service.persistence.CardLearningStateUpdateService;
import com.khaleo.flashcard.service.persistence.PersistenceValidationExceptionMapper;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudyRatingService {

    private final StudyAccessService studyAccessService;
    private final CardLearningStateRepository cardLearningStateRepository;
    private final CardLearningStateUpdateService cardLearningStateUpdateService;
    private final UserRepository userRepository;
    private final StudySchedulerService studySchedulerService;
    private final StudyActivityLogPublisher studyActivityLogPublisher;
    private final PersistenceValidationExceptionMapper exceptionMapper;
    private final NewRelicDeckMediaInstrumentation instrumentation;

    @Transactional
    public RateCardResponse rateCard(UUID cardId, RateCardRequest request) {
        StudyAccessService.CardAccessContext accessContext = studyAccessService.requireCardAccess(cardId);
        UUID userId = accessContext.actorId();
        Card card = accessContext.card();

        if (request.rating() == null) {
            throw new IllegalArgumentException("rating is required");
        }
        if (request.timeSpentMs() == null || request.timeSpentMs() < 0) {
            throw new IllegalArgumentException("timeSpentMs must be non-negative");
        }

        Instant now = Instant.now();

        try {
            CardLearningState saved = cardLearningStateUpdateService.saveWithSingleRetry(
                    userId,
                    cardId,
                    () -> {
                        CardLearningState current = cardLearningStateRepository
                                .findByUserIdAndCardId(userId, cardId)
                                .orElseGet(() -> CardLearningState.builder()
                                        .user(userRepository.findById(userId)
                                                .orElseThrow(() -> exceptionMapper.missingRelationship("user", userId.toString())))
                                        .card(card)
                                        .build());

                        SpacedRepetitionService.RatingOutcome outcome = studySchedulerService.apply(current, request.rating(), now);
                        current.setState(outcome.state());
                        current.setIntervalInDays(outcome.intervalInDays());
                        current.setEaseFactor(outcome.easeFactor());
                        current.setNextReviewDate(outcome.nextReviewAt());
                        current.setLastReviewedAt(outcome.lastReviewedAt());
                        current.setLearningStepGoodCount(outcome.learningStepGoodCount());

                        return cardLearningStateRepository.saveAndFlush(current);
                    });

            studyActivityLogPublisher.publishRatingEvent(
                    userId,
                    cardId,
                    card.getDeck().getId(),
                    request.rating(),
                    request.timeSpentMs(),
                    saved.getIntervalInDays(),
                    saved.getEaseFactor());

            instrumentation.recordStudyRatingOutcome("success", Map.of(
                    "userId", userId,
                    "cardId", cardId,
                    "rating", request.rating().name(),
                    "state", saved.getState().name()));

            return new RateCardResponse(
                    cardId,
                    saved.getState(),
                    saved.getNextReviewDate(),
                    saved.getIntervalInDays(),
                    saved.getEaseFactor());
        } catch (RuntimeException ex) {
            instrumentation.recordStudyRatingFailure(ex.getClass().getSimpleName(), Map.of(
                    "userId", userId,
                    "cardId", cardId,
                    "rating", request.rating().name()));
            log.error("event=study_rating_failed userId={} cardId={} reason={}", userId, cardId, ex.getMessage(), ex);
            throw ex;
        }
    }
}
