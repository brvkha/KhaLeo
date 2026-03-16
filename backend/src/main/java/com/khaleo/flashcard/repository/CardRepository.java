package com.khaleo.flashcard.repository;

import com.khaleo.flashcard.entity.Card;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CardRepository extends JpaRepository<Card, UUID> {

    List<Card> findByDeckId(UUID deckId);

    Page<Card> findByDeckId(UUID deckId, Pageable pageable);

    @Query("""
            select c from Card c
            where c.deck.id = :deckId
                and (:frontText is null or lower(coalesce(c.frontText, '')) like lower(concat('%', :frontText, '%')))
                and (:backText is null or lower(coalesce(c.backText, '')) like lower(concat('%', :backText, '%')))
                and (:vocabulary is null or lower(coalesce(c.frontText, '')) = lower(:vocabulary) or lower(coalesce(c.backText, '')) = lower(:vocabulary))
            """)
    Page<Card> searchInDeck(
            @Param("deckId") UUID deckId,
            @Param("frontText") String frontText,
            @Param("backText") String backText,
            @Param("vocabulary") String vocabulary,
            Pageable pageable);

    @Modifying
    void deleteByDeckId(UUID deckId);
}
