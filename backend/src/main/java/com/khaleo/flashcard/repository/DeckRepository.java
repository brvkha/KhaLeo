package com.khaleo.flashcard.repository;

import com.khaleo.flashcard.entity.Deck;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DeckRepository extends JpaRepository<Deck, UUID> {

    List<Deck> findByAuthorId(UUID authorId);

    Page<Deck> findByAuthorId(UUID authorId, Pageable pageable);

    Page<Deck> findByIsPublicTrue(Pageable pageable);

    Page<Deck> findByAuthorIdAndIsPublic(UUID authorId, Boolean isPublic, Pageable pageable);

        Optional<Deck> findByIdAndIsPublicTrue(UUID id);

        @Query("""
                        select d from Deck d
                        where d.isPublic = true
                            and (:queryText is null or lower(coalesce(d.name, '')) like lower(concat('%', :queryText, '%')))
                        """)
        Page<Deck> searchPublicDecks(@Param("queryText") String queryText, Pageable pageable);

    Page<Deck> findByAuthorIdAndIsPublicFalse(UUID authorId, Pageable pageable);

    @Query("""
            select d from Deck d
            where d.author.id = :authorId
                and d.isPublic = false
                and (:queryText is null or lower(coalesce(d.name, '')) like lower(concat('%', :queryText, '%')))
            """)
    Page<Deck> findPrivateOwnedDecks(
            @Param("authorId") UUID authorId,
            @Param("queryText") String queryText,
            Pageable pageable);
}
