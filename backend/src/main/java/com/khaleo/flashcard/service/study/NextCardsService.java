package com.khaleo.flashcard.service.study;

import com.khaleo.flashcard.config.observability.NewRelicDeckMediaInstrumentation;
import com.khaleo.flashcard.entity.Card;
import com.khaleo.flashcard.entity.CardLearningState;
import com.khaleo.flashcard.entity.enums.CardLearningStateType;
import com.khaleo.flashcard.model.study.NextCardsPageResponse;
import com.khaleo.flashcard.model.study.NextCardsRequest;
import com.khaleo.flashcard.model.study.StudyCardSummary;
import com.khaleo.flashcard.model.study.StudyPaginationToken;
import com.khaleo.flashcard.repository.CardLearningStateRepository;
import com.khaleo.flashcard.repository.CardRepository;
import com.khaleo.flashcard.service.persistence.PersistenceValidationExceptionMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NextCardsService {

    private final StudyAccessService studyAccessService;
    private final StudySessionLimitService studySessionLimitService;
    private final CardLearningStateRepository cardLearningStateRepository;
    private final CardRepository cardRepository;
    private final PersistenceValidationExceptionMapper exceptionMapper;
    private final NewRelicDeckMediaInstrumentation instrumentation;

    @Value("${app.study.next-cards.default-page-size:20}")
    private int defaultPageSize;

    @Value("${app.study.next-cards.max-page-size:100}")
    private int maxPageSize;

    public NextCardsPageResponse getNextCards(UUID deckId, NextCardsRequest request) {
        StudyAccessService.DeckAccessContext context = studyAccessService.requireDeckAccess(deckId);

        final int size;
        final StudyPaginationToken token;
        try {
            size = request.resolvedSize(defaultPageSize, maxPageSize);
            token = request.resolvedToken();
        } catch (IllegalArgumentException ex) {
            throw exceptionMapper.invalidPagination(0, request.size());
        }

        Instant now = Instant.now();
        UUID userId = context.actorId();

        List<CardLearningState> dueLearning = cardLearningStateRepository
                .findByUserIdAndCardDeckIdAndStateAndNextReviewDateLessThanEqualOrderByNextReviewDateAsc(
                        userId,
                        deckId,
                        CardLearningStateType.LEARNING,
                        now);

        List<CardLearningState> dueReview = cardLearningStateRepository
                .findByUserIdAndCardDeckIdAndStateInAndNextReviewDateLessThanEqualOrderByNextReviewDateAsc(
                        userId,
                        deckId,
                        List.of(CardLearningStateType.REVIEW, CardLearningStateType.MASTERED),
                        now);

        int newQuota = studySessionLimitService.remainingNewCardQuota(userId);
        List<Card> newCards = newQuota <= 0
                ? List.of()
                : cardRepository.findUnseenCardsInDeck(deckId, userId, PageRequest.of(0, newQuota));

        List<StudyCardSummary> ordered = new ArrayList<>(dueLearning.size() + dueReview.size() + newCards.size());
        dueLearning.forEach(state -> ordered.add(StudyCardSummary.fromLearningState(state, "LEARNING_DUE")));
        dueReview.forEach(state -> ordered.add(StudyCardSummary.fromLearningState(state, "REVIEW_DUE")));
        newCards.forEach(card -> ordered.add(StudyCardSummary.fromNewCard(card)));

        int fromIndex = Math.min(token.offset(), ordered.size());
        int toIndex = Math.min(fromIndex + size, ordered.size());

        List<StudyCardSummary> pageItems = ordered.subList(fromIndex, toIndex);
        boolean hasMore = toIndex < ordered.size();
        String nextToken = hasMore ? new StudyPaginationToken(toIndex).encode() : null;

        Map<String, Object> attrs = new HashMap<>();
        attrs.put("deckId", deckId);
        attrs.put("userId", userId);
        attrs.put("requestedSize", size);
        attrs.put("returnedSize", pageItems.size());
        attrs.put("offset", token.offset());
        attrs.put("hasMore", hasMore);
        attrs.put("newQuota", newQuota);

        instrumentation.recordStudyNextCardsOutcome("success", attrs);
        log.info("event=study_next_cards_success deckId={} userId={} size={} returned={} offset={} hasMore={}",
                deckId,
                userId,
                size,
                pageItems.size(),
                token.offset(),
                hasMore);

        return new NextCardsPageResponse(pageItems, nextToken, hasMore);
    }
}
