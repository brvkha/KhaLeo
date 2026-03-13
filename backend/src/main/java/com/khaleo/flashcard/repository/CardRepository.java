package com.khaleo.flashcard.repository;

import com.khaleo.flashcard.entity.Card;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardRepository extends JpaRepository<Card, UUID> {

    List<Card> findByDeckId(UUID deckId);
}
