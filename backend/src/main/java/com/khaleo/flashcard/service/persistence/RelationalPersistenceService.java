package com.khaleo.flashcard.service.persistence;

import com.khaleo.flashcard.entity.Card;
import com.khaleo.flashcard.entity.CardLearningState;
import com.khaleo.flashcard.entity.Deck;
import com.khaleo.flashcard.entity.User;
import com.khaleo.flashcard.entity.enums.CardLearningStateType;
import com.khaleo.flashcard.repository.CardLearningStateRepository;
import com.khaleo.flashcard.repository.CardRepository;
import com.khaleo.flashcard.repository.DeckRepository;
import com.khaleo.flashcard.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class RelationalPersistenceService {

    private final UserRepository userRepository;
    private final DeckRepository deckRepository;
    private final CardRepository cardRepository;
    private final CardLearningStateRepository cardLearningStateRepository;

    public User createUser(CreateUserRequest request) {
        try {
            User user = User.builder()
                    .email(request.email())
                    .passwordHash(request.passwordHash())
                    .build();

            User saved = userRepository.save(user);
            log.info("event=relational_user_create_success userId={} email={}", saved.getId(), saved.getEmail());
            return saved;
        } catch (RuntimeException ex) {
            log.error("event=relational_user_create_failed email={} reason={}", request.email(), ex.getMessage(), ex);
            throw ex;
        }
    }

    public Deck createDeck(UUID authorId, CreateDeckRequest request) {
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + authorId));

        try {
            Deck deck = Deck.builder()
                    .author(author)
                    .name(request.name())
                    .description(request.description())
                    .coverImageUrl(request.coverImageUrl())
                    .isPublic(request.isPublic())
                    .tags(request.tags())
                    .build();

            Deck saved = deckRepository.save(deck);
            log.info("event=relational_deck_create_success deckId={} authorId={}", saved.getId(), authorId);
            return saved;
        } catch (RuntimeException ex) {
            log.error("event=relational_deck_create_failed authorId={} reason={}", authorId, ex.getMessage(), ex);
            throw ex;
        }
    }

    public Card createCard(UUID deckId, CreateCardRequest request) {
        Deck deck = deckRepository.findById(deckId)
                .orElseThrow(() -> new EntityNotFoundException("Deck not found: " + deckId));

        try {
            Card card = Card.builder()
                    .deck(deck)
                    .frontText(request.frontText())
                    .frontMediaUrl(request.frontMediaUrl())
                    .backText(request.backText())
                    .backMediaUrl(request.backMediaUrl())
                    .build();

            Card saved = cardRepository.save(card);
            log.info("event=relational_card_create_success cardId={} deckId={}", saved.getId(), deckId);
            return saved;
        } catch (RuntimeException ex) {
            log.error("event=relational_card_create_failed deckId={} reason={}", deckId, ex.getMessage(), ex);
            throw ex;
        }
    }

    public CardLearningState upsertLearningState(UpsertLearningStateRequest request) {
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + request.userId()));
        Card card = cardRepository.findById(request.cardId())
                .orElseThrow(() -> new EntityNotFoundException("Card not found: " + request.cardId()));

        CardLearningState state = cardLearningStateRepository
                .findByUserIdAndCardId(request.userId(), request.cardId())
                .orElseGet(() -> CardLearningState.builder()
                        .user(user)
                        .card(card)
                        .build());

        if (request.state() != null) {
            state.setState(request.state());
        }
        if (request.easeFactor() != null) {
            state.setEaseFactor(request.easeFactor());
        }
        if (request.intervalInDays() != null) {
            state.setIntervalInDays(request.intervalInDays());
        }
        if (request.nextReviewDate() != null) {
            state.setNextReviewDate(request.nextReviewDate());
        }

        try {
            CardLearningState saved = cardLearningStateRepository.saveAndFlush(state);
            log.info("event=relational_learning_state_upsert_success stateId={} userId={} cardId={}",
                    saved.getId(), request.userId(), request.cardId());
            return saved;
        } catch (DataIntegrityViolationException ex) {
            log.error("event=relational_learning_state_upsert_failed userId={} cardId={} reason={}",
                    request.userId(), request.cardId(), ex.getMessage(), ex);
            throw ex;
        }
    }

    public record CreateUserRequest(String email, String passwordHash) {
    }

    public record CreateDeckRequest(
            String name,
            String description,
            String coverImageUrl,
            Boolean isPublic,
            String tags) {
    }

    public record CreateCardRequest(
            String frontText,
            String frontMediaUrl,
            String backText,
            String backMediaUrl) {
    }

    public record UpsertLearningStateRequest(
            UUID userId,
            UUID cardId,
            CardLearningStateType state,
            BigDecimal easeFactor,
            Integer intervalInDays,
            Instant nextReviewDate) {
    }
}
