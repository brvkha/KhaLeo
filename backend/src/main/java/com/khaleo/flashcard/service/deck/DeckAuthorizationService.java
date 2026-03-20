package com.khaleo.flashcard.service.deck;

import com.khaleo.flashcard.entity.Deck;
import com.khaleo.flashcard.repository.DeckRepository;
import com.khaleo.flashcard.service.persistence.DeckCardAccessGuard;
import com.khaleo.flashcard.service.persistence.PersistenceValidationExceptionMapper;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DeckAuthorizationService {

    private final DeckCardAccessGuard deckCardAccessGuard;
    private final DeckRepository deckRepository;
    private final PersistenceValidationExceptionMapper exceptionMapper;

    public UUID requireActorId(String operation, String resourceType, String resourceKey) {
        return deckCardAccessGuard.requireAuthenticatedUserId(operation, resourceType, resourceKey);
    }

    public Deck requireOwnedPrivateDeck(UUID actorId, UUID deckId, String operation) {
        Deck deck = deckRepository.findById(deckId).orElseThrow(() -> exceptionMapper.deckNotFound(deckId));
        if (Boolean.TRUE.equals(deck.getIsPublic()) || !actorId.equals(deck.getAuthor().getId())) {
            throw exceptionMapper.authorizationDenied(operation, "deck", deckId.toString());
        }
        return deck;
    }
}
