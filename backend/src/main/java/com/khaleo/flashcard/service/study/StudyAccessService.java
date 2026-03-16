package com.khaleo.flashcard.service.study;

import com.khaleo.flashcard.entity.Card;
import com.khaleo.flashcard.entity.Deck;
import com.khaleo.flashcard.repository.CardRepository;
import com.khaleo.flashcard.repository.DeckRepository;
import com.khaleo.flashcard.service.persistence.DeckCardAccessGuard;
import com.khaleo.flashcard.service.persistence.PersistenceValidationExceptionMapper;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StudyAccessService {

    private final DeckRepository deckRepository;
    private final CardRepository cardRepository;
    private final DeckCardAccessGuard deckCardAccessGuard;
    private final PersistenceValidationExceptionMapper exceptionMapper;

    public DeckAccessContext requireDeckAccess(UUID deckId) {
        Deck deck = deckRepository.findById(deckId)
                .orElseThrow(() -> exceptionMapper.deckNotFound(deckId));
        deckCardAccessGuard.ensureCanReadDeck(deck);
        UUID actorId = deckCardAccessGuard.requireAuthenticatedUserId("read", "deck", deckId.toString());
        return new DeckAccessContext(actorId, deck);
    }

    public CardAccessContext requireCardAccess(UUID cardId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> exceptionMapper.cardNotFound(cardId));
        Deck deck = card.getDeck();
        deckCardAccessGuard.ensureCanReadDeck(deck);
        UUID actorId = deckCardAccessGuard.requireAuthenticatedUserId("read", "card", cardId.toString());
        return new CardAccessContext(actorId, card, deck);
    }

    public record DeckAccessContext(UUID actorId, Deck deck) {
    }

    public record CardAccessContext(UUID actorId, Card card, Deck deck) {
    }
}
