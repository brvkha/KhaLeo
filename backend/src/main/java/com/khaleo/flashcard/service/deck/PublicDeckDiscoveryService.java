package com.khaleo.flashcard.service.deck;

import com.khaleo.flashcard.config.PaginationConfig;
import com.khaleo.flashcard.controller.deck.dto.PublicDeckDetailResponse;
import com.khaleo.flashcard.controller.deck.dto.PublicDeckSummaryResponse;
import com.khaleo.flashcard.entity.Card;
import com.khaleo.flashcard.entity.Deck;
import com.khaleo.flashcard.repository.CardRepository;
import com.khaleo.flashcard.repository.DeckRepository;
import com.khaleo.flashcard.service.persistence.PersistenceValidationExceptionMapper;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PublicDeckDiscoveryService {

    private final DeckRepository deckRepository;
    private final CardRepository cardRepository;
    private final PaginationConfig paginationConfig;
    private final PersistenceValidationExceptionMapper exceptionMapper;

    @Transactional(readOnly = true)
    public Page<PublicDeckSummaryResponse> listPublicDecks(String query, Integer page, Integer size) {
        String normalizedQuery = query == null || query.isBlank() ? null : query.trim();
        return deckRepository.searchPublicDecks(normalizedQuery, paginationConfig.resolvePageable(page, size))
                .map(this::toSummary);
    }

    @Transactional(readOnly = true)
    public PublicDeckDetailResponse getPublicDeckDetail(UUID deckId) {
        Deck deck = deckRepository.findByIdAndIsPublicTrue(deckId)
                .orElseThrow(() -> exceptionMapper.deckNotFound(deckId));

        List<PublicDeckDetailResponse.CardPreviewItem> previews = cardRepository
                .findByDeckId(deckId, PageRequest.of(0, 20))
                .stream()
                .map(card -> new PublicDeckDetailResponse.CardPreviewItem(card.getId(), card.getFrontText()))
                .toList();

        return new PublicDeckDetailResponse(
                deck.getId(),
                deck.getName(),
                deck.getDescription(),
                deck.getAuthor().getEmail(),
                splitTags(deck.getTags()),
                (int) cardRepository.countByDeckId(deckId),
                previews);
    }

    private PublicDeckSummaryResponse toSummary(Deck deck) {
        int cardCount = (int) cardRepository.countByDeckId(deck.getId());
        return new PublicDeckSummaryResponse(
                deck.getId(),
                deck.getName(),
                deck.getAuthor().getEmail(),
                deck.getDescription(),
                splitTags(deck.getTags()),
                cardCount,
                deck.getUpdatedAt());
    }

    private List<String> splitTags(String tags) {
        if (tags == null || tags.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(tags.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
    }
}
