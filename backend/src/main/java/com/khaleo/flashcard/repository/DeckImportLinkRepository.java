package com.khaleo.flashcard.repository;

import com.khaleo.flashcard.entity.DeckImportLink;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DeckImportLinkRepository extends JpaRepository<DeckImportLink, UUID> {

    Optional<DeckImportLink> findBySourceDeckIdAndTargetPrivateDeckId(UUID sourceDeckId, UUID targetPrivateDeckId);

    Optional<DeckImportLink> findBySourceDeckIdAndImportedByUserId(UUID sourceDeckId, UUID importedByUserId);

    @Query("""
            select dil from DeckImportLink dil
            join fetch dil.sourceDeck sd
            join fetch dil.targetPrivateDeck td
            where dil.id = :id
            """)
    Optional<DeckImportLink> findWithDecksById(@Param("id") UUID id);
}
