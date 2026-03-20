package com.khaleo.flashcard.service.importmerge;

import com.khaleo.flashcard.entity.Card;
import com.khaleo.flashcard.entity.Deck;
import com.khaleo.flashcard.entity.DeckImportLink;
import com.khaleo.flashcard.entity.enums.ImportMergeStatus;
import com.khaleo.flashcard.repository.CardRepository;
import com.khaleo.flashcard.repository.DeckImportLinkRepository;
import com.khaleo.flashcard.repository.DeckRepository;
import com.khaleo.flashcard.service.persistence.PersistenceValidationExceptionMapper;
import com.khaleo.flashcard.service.persistence.RelationalPersistenceService;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PublicDeckImportService {

    private final DeckRepository deckRepository;
    private final CardRepository cardRepository;
    private final DeckImportLinkRepository deckImportLinkRepository;
    private final RelationalPersistenceService relationalPersistenceService;
    private final PersistenceValidationExceptionMapper exceptionMapper;

    @Transactional
    public ImportResult importPublicDeck(UUID actorId, UUID sourceDeckId) {
        Deck source = deckRepository.findById(sourceDeckId)
                .orElseThrow(() -> exceptionMapper.deckNotFound(sourceDeckId));
        if (!Boolean.TRUE.equals(source.getIsPublic())) {
            throw exceptionMapper.deckNotPublic(sourceDeckId);
        }

        DeckImportLink existing = deckImportLinkRepository
                .findBySourceDeckIdAndImportedByUserId(sourceDeckId, actorId)
                .orElse(null);
        if (existing != null) {
            return new ImportResult(
                    existing.getId(),
                    sourceDeckId,
                    existing.getTargetPrivateDeck().getId(),
                    existing.getLastMergeStatus(),
                    0);
        }

        Deck target = relationalPersistenceService.createDeck(
                actorId,
                new RelationalPersistenceService.CreateDeckRequest(
                        source.getName(),
                        source.getDescription(),
                        source.getCoverImageUrl(),
                        false,
                        source.getTags()));

        for (Card sourceCard : cardRepository.findByDeckId(sourceDeckId)) {
            relationalPersistenceService.createCard(
                    target.getId(),
                    new RelationalPersistenceService.CreateCardRequest(
                            sourceCard.getFrontText(),
                            sourceCard.getFrontMediaUrl(),
                            sourceCard.getBackText(),
                            sourceCard.getBackMediaUrl()));
        }

        DeckImportLink created = deckImportLinkRepository.save(DeckImportLink.builder()
                .sourceDeck(source)
                .targetPrivateDeck(target)
                .importedByUserId(actorId)
                .lastImportedAt(Instant.now())
                .lastMergeStatus(ImportMergeStatus.SUCCESS)
                .build());

        return new ImportResult(created.getId(), sourceDeckId, target.getId(), ImportMergeStatus.SUCCESS, 0);
    }

    public record ImportResult(
            UUID importLinkId,
            UUID sourceDeckId,
            UUID targetPrivateDeckId,
            ImportMergeStatus status,
            int conflictsCount) {
    }
}
