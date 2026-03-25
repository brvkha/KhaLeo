package com.khaleo.flashcard.service.study;

import com.khaleo.flashcard.config.observability.NewRelicDeckMediaInstrumentation;
import com.khaleo.flashcard.entity.Card;
import com.khaleo.flashcard.entity.CardLearningState;
import com.khaleo.flashcard.entity.enums.CardLearningStateType;
import com.khaleo.flashcard.model.dynamo.RatingGiven;
import com.khaleo.flashcard.model.study.RateCardRequest;
import com.khaleo.flashcard.model.study.RateCardPreviewResponse;
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
                        current.setIntervalInDays(outcome.scheduledDays());
                        current.setNextReviewDate(outcome.nextReviewAt());
                        current.setLastReviewedAt(outcome.lastReviewedAt());
                        current.setFsrsScheduledDays(outcome.scheduledDays());
                        current.setFsrsStability(outcome.stability());
                        current.setFsrsDifficulty(outcome.difficulty());
                        current.setFsrsElapsedDays(outcome.elapsedDays());
                        current.setFsrsReps(outcome.reps());
                        current.setFsrsLapses(outcome.lapses());
                        if (request.rating() == RatingGiven.GOOD
                                && (outcome.state() == CardLearningStateType.LEARNING
                                || outcome.state() == CardLearningStateType.RELEARNING)) {
                            current.setLearningStepGoodCount(defaultZero(current.getLearningStepGoodCount()) + 1);
                        } else {
                            current.setLearningStepGoodCount(0);
                        }

                        return cardLearningStateRepository.saveAndFlush(current);
                    });

            studyActivityLogPublisher.publishRatingEvent(
                    userId,
                    cardId,
                    card.getDeck().getId(),
                    request.rating(),
                    request.timeSpentMs(),
                    saved.getFsrsScheduledDays(),
                    saved.getFsrsStability(),
                    saved.getFsrsDifficulty());

            instrumentation.recordStudyRatingOutcome("success", Map.of(
                    "userId", userId,
                    "cardId", cardId,
                    "rating", request.rating().name(),
                    "state", saved.getState().name()));

            return new RateCardResponse(
                    cardId,
                    saved.getState(),
                    saved.getNextReviewDate(),
                    saved.getFsrsScheduledDays(),
                    saved.getFsrsStability(),
                    saved.getFsrsDifficulty());
        } catch (RuntimeException ex) {
            instrumentation.recordStudyRatingFailure(ex.getClass().getSimpleName(), Map.of(
                    "userId", userId,
                    "cardId", cardId,
                    "rating", request.rating().name()));
            log.error("event=study_rating_failed userId={} cardId={} reason={}", userId, cardId, ex.getMessage(), ex);
            throw ex;
        }
    }

        @Transactional(readOnly = true)
        public RateCardPreviewResponse previewCardRatings(UUID cardId) {
        StudyAccessService.CardAccessContext accessContext = studyAccessService.requireCardAccess(cardId);
        UUID userId = accessContext.actorId();
        Instant now = Instant.now();

        CardLearningState current = cardLearningStateRepository
            .findByUserIdAndCardId(userId, cardId)
            .orElseGet(() -> CardLearningState.builder()
                .state(CardLearningStateType.NEW)
                .learningStepGoodCount(0)
                .build());

        SpacedRepetitionService.RatingOutcome againOutcome = studySchedulerService.apply(copyCardState(current), RatingGiven.AGAIN, now);
        SpacedRepetitionService.RatingOutcome hardOutcome = studySchedulerService.apply(copyCardState(current), RatingGiven.HARD, now);
        SpacedRepetitionService.RatingOutcome goodOutcome = studySchedulerService.apply(copyCardState(current), RatingGiven.GOOD, now);
        SpacedRepetitionService.RatingOutcome easyOutcome = studySchedulerService.apply(copyCardState(current), RatingGiven.EASY, now);

        return new RateCardPreviewResponse(
            toPreview(againOutcome),
            toPreview(hardOutcome),
            toPreview(goodOutcome),
            toPreview(easyOutcome));
        }

    private int defaultZero(Integer value) {
        return value == null ? 0 : value;
    }

    private RateCardPreviewResponse.RatingPreview toPreview(SpacedRepetitionService.RatingOutcome outcome) {
        return new RateCardPreviewResponse.RatingPreview(
                outcome.nextReviewAt(),
                outcome.scheduledDays(),
                outcome.state().name());
    }

    private CardLearningState copyCardState(CardLearningState original) {
        return CardLearningState.builder()
                .state(original.getState())
                .learningStepGoodCount(defaultZero(original.getLearningStepGoodCount()))
                .fsrsStability(original.getFsrsStability())
                .fsrsDifficulty(original.getFsrsDifficulty())
                .fsrsReps(original.getFsrsReps())
                .fsrsLapses(original.getFsrsLapses())
                .lastReviewedAt(original.getLastReviewedAt())
                .fsrsElapsedDays(original.getFsrsElapsedDays())
                .build();
    }
}
