package com.khaleo.flashcard.model.study;

import com.khaleo.flashcard.entity.Card;
import com.khaleo.flashcard.entity.CardLearningState;
import com.khaleo.flashcard.entity.enums.CardLearningStateType;
import java.time.Instant;
import java.util.UUID;

public record StudyCardSummary(
        UUID cardId,
        UUID deckId,
        String frontText,
        String backText,
        CardLearningStateType state,
        Instant nextReviewDate,
        String sourceTier) {

    public static StudyCardSummary fromLearningState(CardLearningState state, String sourceTier) {
        Card card = state.getCard();
        return new StudyCardSummary(
                card.getId(),
                card.getDeck().getId(),
                card.getFrontText(),
                card.getBackText(),
                state.getState(),
                state.getNextReviewDate(),
                sourceTier);
    }

    public static StudyCardSummary fromNewCard(Card card) {
        return new StudyCardSummary(
                card.getId(),
                card.getDeck().getId(),
                card.getFrontText(),
                card.getBackText(),
                CardLearningStateType.NEW,
                null,
                "NEW");
    }
}
