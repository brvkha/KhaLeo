package com.khaleo.flashcard.service.deck;

import com.khaleo.flashcard.entity.Card;
import com.khaleo.flashcard.entity.Deck;
import com.khaleo.flashcard.service.persistence.RelationalPersistenceService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PrivateDeckCrudService {

    private final DeckAuthorizationService deckAuthorizationService;
    private final RelationalPersistenceService relationalPersistenceService;

    @Transactional
    public Deck createPrivateDeck(UUID actorId, RelationalPersistenceService.CreateDeckRequest request) {
        return relationalPersistenceService.createDeck(actorId, new RelationalPersistenceService.CreateDeckRequest(
                request.name(),
                request.description(),
                request.coverImageUrl(),
                false,
                request.tags()));
    }

    @Transactional
    public Deck updatePrivateDeck(UUID actorId, UUID deckId, RelationalPersistenceService.UpdateDeckRequest request) {
        deckAuthorizationService.requireOwnedPrivateDeck(actorId, deckId, "update");
        return relationalPersistenceService.updateDeck(deckId, request);
    }

    @Transactional
    public void deletePrivateDeck(UUID actorId, UUID deckId) {
        deckAuthorizationService.requireOwnedPrivateDeck(actorId, deckId, "delete");
        relationalPersistenceService.deleteDeck(deckId);
    }

    @Transactional(readOnly = true)
    public Page<Card> searchCards(UUID actorId, UUID deckId, RelationalPersistenceService.CardSearchQuery query) {
        deckAuthorizationService.requireOwnedPrivateDeck(actorId, deckId, "search");
        return relationalPersistenceService.searchCardsInDeck(deckId, query);
    }
}
