package com.khaleo.flashcard.repository;

import com.khaleo.flashcard.entity.Deck;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeckRepository extends JpaRepository<Deck, UUID> {

    List<Deck> findByAuthorId(UUID authorId);

    Page<Deck> findByAuthorId(UUID authorId, Pageable pageable);

    Page<Deck> findByIsPublicTrue(Pageable pageable);

    Page<Deck> findByAuthorIdAndIsPublic(UUID authorId, Boolean isPublic, Pageable pageable);
}
