package com.khaleo.flashcard.repository;

import com.khaleo.flashcard.entity.CardLearningState;
import com.khaleo.flashcard.entity.enums.CardLearningStateType;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardLearningStateRepository extends JpaRepository<CardLearningState, UUID> {

    Optional<CardLearningState> findByUserIdAndCardId(UUID userId, UUID cardId);

    @Query("""
        SELECT cls FROM CardLearningState cls
        JOIN FETCH cls.card c
        JOIN FETCH c.deck
        WHERE cls.user.id = ?1
          AND c.deck.id = ?2
          AND cls.state = ?3
          AND cls.nextReviewDate <= ?4
        ORDER BY cls.nextReviewDate ASC
        """)
    List<CardLearningState> findByUserIdAndCardDeckIdAndStateAndNextReviewDateLessThanEqualOrderByNextReviewDateAsc(
        UUID userId,
        UUID deckId,
        CardLearningStateType state,
        Instant dueAt);

    @Query("""
        SELECT cls FROM CardLearningState cls
        JOIN FETCH cls.card c
        JOIN FETCH c.deck
        WHERE cls.user.id = ?1
          AND c.deck.id = ?2
          AND cls.state IN (?3)
          AND cls.nextReviewDate <= ?4
        ORDER BY cls.nextReviewDate ASC
        """)
    List<CardLearningState> findByUserIdAndCardDeckIdAndStateInAndNextReviewDateLessThanEqualOrderByNextReviewDateAsc(
        UUID userId,
        UUID deckId,
        Collection<CardLearningStateType> states,
        Instant dueAt);

        long countByUserIdAndStateNotAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            UUID userId,
            CardLearningStateType excludedState,
            Instant startInclusive,
            Instant endExclusive);

        long countByLastReviewedAtGreaterThanEqual(Instant sinceInclusive);

    @Modifying
    void deleteByCardIdIn(Collection<UUID> cardIds);
}
