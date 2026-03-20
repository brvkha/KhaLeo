package com.khaleo.flashcard.service.deck;

import com.khaleo.flashcard.config.PaginationConfig;
import com.khaleo.flashcard.entity.Deck;
import com.khaleo.flashcard.repository.DeckRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PrivateWorkspaceService {

    private final DeckRepository deckRepository;
    private final PaginationConfig paginationConfig;

    @Transactional(readOnly = true)
    public Page<Deck> listOwnedPrivateDecks(UUID actorId, String query, Integer page, Integer size) {
        Pageable pageable = paginationConfig.resolvePageable(page, size);
        String normalizedQuery = query == null || query.isBlank() ? null : query.trim();
        return deckRepository.findPrivateOwnedDecks(actorId, normalizedQuery, pageable);
    }
}
