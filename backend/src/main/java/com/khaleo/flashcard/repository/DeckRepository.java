package com.khaleo.flashcard.repository;

import com.khaleo.flashcard.entity.Deck;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeckRepository extends JpaRepository<Deck, UUID> {

    List<Deck> findByAuthorId(UUID authorId);
}
