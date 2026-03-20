package com.khaleo.flashcard.repository;

import com.khaleo.flashcard.entity.ReimportMergeConflict;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReimportMergeConflictRepository extends JpaRepository<ReimportMergeConflict, UUID> {

    List<ReimportMergeConflict> findByDeckImportLinkId(UUID deckImportLinkId);

    List<ReimportMergeConflict> findByDeckImportLinkIdAndResolutionChoiceIsNull(UUID deckImportLinkId);

    Optional<ReimportMergeConflict> findByIdAndDeckImportLinkId(UUID id, UUID deckImportLinkId);

    void deleteByDeckImportLinkId(UUID deckImportLinkId);
}
