package com.khaleo.flashcard.service.importmerge;

import com.khaleo.flashcard.entity.Deck;
import com.khaleo.flashcard.entity.DeckImportLink;
import com.khaleo.flashcard.entity.ReimportMergeConflict;
import com.khaleo.flashcard.entity.enums.ImportMergeStatus;
import com.khaleo.flashcard.entity.enums.ReimportConflictResolutionChoice;
import com.khaleo.flashcard.repository.DeckImportLinkRepository;
import com.khaleo.flashcard.repository.DeckRepository;
import com.khaleo.flashcard.repository.ReimportMergeConflictRepository;
import com.khaleo.flashcard.service.persistence.PersistenceValidationExceptionMapper;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ConflictResolutionService {

    private final ReimportMergeConflictRepository conflictRepository;
    private final ReimportMergeService reimportMergeService;
    private final DeckImportLinkRepository deckImportLinkRepository;
    private final DeckRepository deckRepository;
    private final PersistenceValidationExceptionMapper exceptionMapper;

    @Transactional(readOnly = true)
    public List<ReimportMergeConflict> listUnresolved(UUID actorId, UUID importLinkId) {
        reimportMergeService.requireOwnedLink(actorId, importLinkId);
        return conflictRepository.findByDeckImportLinkIdAndResolutionChoiceIsNull(importLinkId);
    }

    @Transactional
    public ResolveResult resolve(
            UUID actorId,
            UUID importLinkId,
            UUID conflictId,
            ReimportConflictResolutionChoice choice) {
        if (choice == null) {
            throw exceptionMapper.invalidConflictResolutionChoice("null");
        }

        ReimportMergeConflict conflict = conflictRepository.findByIdAndDeckImportLinkId(conflictId, importLinkId)
                .orElseThrow(() -> exceptionMapper.importLinkNotFound(importLinkId));

        DeckImportLink link = reimportMergeService.requireOwnedLink(actorId, importLinkId);

        conflict.setResolutionChoice(choice);
        conflict.setResolvedAt(Instant.now());
        conflictRepository.save(conflict);

        int unresolved = conflictRepository.findByDeckImportLinkIdAndResolutionChoiceIsNull(importLinkId).size();
        if (unresolved == 0) {
            applyCloudChoices(link);
            link.setLastImportedAt(Instant.now());
            link.setLastMergeStatus(ImportMergeStatus.SUCCESS);
            deckImportLinkRepository.save(link);
        }

        return new ResolveResult(importLinkId, conflictId, choice, unresolved);
    }

    private void applyCloudChoices(DeckImportLink link) {
        Deck source = link.getSourceDeck();
        Deck target = link.getTargetPrivateDeck();

        List<ReimportMergeConflict> allConflicts = conflictRepository.findByDeckImportLinkId(link.getId());
        for (ReimportMergeConflict conflict : allConflicts) {
            if (conflict.getResolutionChoice() != ReimportConflictResolutionChoice.CLOUD) {
                continue;
            }
            switch (conflict.getFieldPath()) {
                case "name" -> target.setName(source.getName());
                case "description" -> target.setDescription(source.getDescription());
                case "tags" -> target.setTags(source.getTags());
                case "coverImageUrl" -> target.setCoverImageUrl(source.getCoverImageUrl());
                default -> {
                    // Ignore unknown fields to keep merge deterministic for known deck metadata.
                }
            }
        }
        deckRepository.save(target);
    }

    public record ResolveResult(
            UUID importLinkId,
            UUID conflictId,
            ReimportConflictResolutionChoice choice,
            int remainingConflicts) {
    }
}
