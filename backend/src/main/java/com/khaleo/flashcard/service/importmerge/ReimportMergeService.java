package com.khaleo.flashcard.service.importmerge;

import com.khaleo.flashcard.entity.Deck;
import com.khaleo.flashcard.entity.DeckImportLink;
import com.khaleo.flashcard.entity.ReimportMergeConflict;
import com.khaleo.flashcard.entity.enums.ImportMergeStatus;
import com.khaleo.flashcard.entity.enums.ReimportConflictScope;
import com.khaleo.flashcard.repository.DeckImportLinkRepository;
import com.khaleo.flashcard.repository.DeckRepository;
import com.khaleo.flashcard.repository.ReimportMergeConflictRepository;
import com.khaleo.flashcard.service.deck.DeckAuthorizationService;
import com.khaleo.flashcard.service.persistence.PersistenceValidationExceptionMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReimportMergeService {

    private final DeckImportLinkRepository deckImportLinkRepository;
    private final ReimportMergeConflictRepository conflictRepository;
    private final DeckAuthorizationService deckAuthorizationService;
    private final DeckRepository deckRepository;
    private final PersistenceValidationExceptionMapper exceptionMapper;

    @Transactional
    public ReimportResult reimport(UUID actorId, UUID importLinkId) {
        DeckImportLink link = requireOwnedLink(actorId, importLinkId);
        Deck source = deckRepository.findById(link.getSourceDeck().getId())
                .orElseThrow(() -> exceptionMapper.deckNotFound(link.getSourceDeck().getId()));
        if (!Boolean.TRUE.equals(source.getIsPublic())) {
            throw exceptionMapper.deckNotPublic(source.getId());
        }

        Deck target = deckAuthorizationService.requireOwnedPrivateDeck(
                actorId,
                link.getTargetPrivateDeck().getId(),
                "reimport");

        conflictRepository.deleteByDeckImportLinkId(importLinkId);
        List<ReimportMergeConflict> conflicts = detectDeckFieldConflicts(link, source, target);

        if (!conflicts.isEmpty()) {
            conflictRepository.saveAll(conflicts);
            link.setLastMergeStatus(ImportMergeStatus.CONFLICT_REQUIRED);
            deckImportLinkRepository.save(link);
            return new ReimportResult(importLinkId, ImportMergeStatus.CONFLICT_REQUIRED, conflicts.size());
        }

        link.setLastImportedAt(Instant.now());
        link.setLastMergeStatus(ImportMergeStatus.SUCCESS);
        deckImportLinkRepository.save(link);
        return new ReimportResult(importLinkId, ImportMergeStatus.SUCCESS, 0);
    }

    public DeckImportLink requireOwnedLink(UUID actorId, UUID importLinkId) {
        DeckImportLink link = deckImportLinkRepository.findWithDecksById(importLinkId)
                .orElseThrow(() -> exceptionMapper.importLinkNotFound(importLinkId));
        if (!actorId.equals(link.getImportedByUserId())) {
            throw exceptionMapper.authorizationDenied("read", "import-link", importLinkId.toString());
        }
        return link;
    }

    private List<ReimportMergeConflict> detectDeckFieldConflicts(DeckImportLink link, Deck source, Deck target) {
        List<ReimportMergeConflict> conflicts = new ArrayList<>();
        maybeAddDeckFieldConflict(conflicts, link, target.getId(), "name", target.getName(), source.getName());
        maybeAddDeckFieldConflict(conflicts, link, target.getId(), "description", target.getDescription(), source.getDescription());
        maybeAddDeckFieldConflict(conflicts, link, target.getId(), "tags", target.getTags(), source.getTags());
        maybeAddDeckFieldConflict(conflicts, link, target.getId(), "coverImageUrl", target.getCoverImageUrl(), source.getCoverImageUrl());
        return conflicts;
    }

    private void maybeAddDeckFieldConflict(
            List<ReimportMergeConflict> conflicts,
            DeckImportLink link,
            UUID targetDeckId,
            String fieldPath,
            String localValue,
            String cloudValue) {
        String normalizedLocal = localValue == null ? "" : localValue;
        String normalizedCloud = cloudValue == null ? "" : cloudValue;
        if (normalizedLocal.equals(normalizedCloud)) {
            return;
        }

        conflicts.add(ReimportMergeConflict.builder()
                .deckImportLink(link)
                .conflictScope(ReimportConflictScope.DECK_FIELD)
                .targetEntityId(targetDeckId.toString())
                .fieldPath(fieldPath)
                .localValueSnapshot(localValue)
                .cloudValueSnapshot(cloudValue)
                .build());
    }

    public record ReimportResult(
            UUID importLinkId,
            ImportMergeStatus status,
            int conflictsCount) {
    }
}
