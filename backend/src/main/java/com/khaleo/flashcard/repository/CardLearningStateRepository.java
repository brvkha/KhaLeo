package com.khaleo.flashcard.repository;

import com.khaleo.flashcard.entity.CardLearningState;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardLearningStateRepository extends JpaRepository<CardLearningState, UUID> {

    Optional<CardLearningState> findByUserIdAndCardId(UUID userId, UUID cardId);

    @Modifying
    void deleteByCardIdIn(Collection<UUID> cardIds);
}
