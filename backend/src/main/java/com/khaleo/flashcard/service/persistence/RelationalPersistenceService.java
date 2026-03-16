package com.khaleo.flashcard.service.persistence;

import com.khaleo.flashcard.entity.Card;
import com.khaleo.flashcard.entity.CardLearningState;
import com.khaleo.flashcard.entity.Deck;
import com.khaleo.flashcard.entity.User;
import com.khaleo.flashcard.entity.enums.CardLearningStateType;
import com.khaleo.flashcard.config.observability.NewRelicDeckMediaInstrumentation;
import com.khaleo.flashcard.model.dynamo.RatingGiven;
import com.khaleo.flashcard.repository.CardLearningStateRepository;
import com.khaleo.flashcard.repository.CardRepository;
import com.khaleo.flashcard.repository.DeckRepository;
import com.khaleo.flashcard.repository.UserRepository;
import com.khaleo.flashcard.service.activitylog.StudyActivityLogPublisher;
import com.khaleo.flashcard.service.media.MediaReferenceService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
@SuppressWarnings("null")
public class RelationalPersistenceService {

    private final UserRepository userRepository;
    private final DeckRepository deckRepository;
    private final CardRepository cardRepository;
    private final CardLearningStateRepository cardLearningStateRepository;
    private final StudyActivityLogPublisher studyActivityLogPublisher;
    private final PersistenceValidationExceptionMapper exceptionMapper;
    private final CardLearningStateUpdateService cardLearningStateUpdateService;
    private final DeckCardAccessGuard deckCardAccessGuard;
    private final MediaReferenceService mediaReferenceService;
    private final NewRelicDeckMediaInstrumentation deckMediaInstrumentation;

    public User createUser(CreateUserRequest request) {
        try {
            User user = User.builder()
                    .email(request.email())
                    .passwordHash(request.passwordHash())
                    .dailyLearningLimit(request.dailyLearningLimit())
                    .build();

            User saved = userRepository.saveAndFlush(user);
            log.info("event=relational_user_create_success userId={} email={}", saved.getId(), saved.getEmail());
            return saved;
        } catch (RuntimeException ex) {
            log.error("event=relational_user_create_failed email={} reason={}", request.email(), ex.getMessage(), ex);
            throw exceptionMapper.mapCreateUserFailure(ex, request.email());
        }
    }

    public Deck createDeck(UUID authorId, CreateDeckRequest request) {
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> exceptionMapper.missingRelationship("user", authorId.toString()));

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
            if (saved.getCoverImageUrl() != null) {
                mediaReferenceService.incrementReference(saved.getCoverImageUrl());
            }
            deckMediaInstrumentation.recordDeckOutcome("create", "success", java.util.Map.of("deckId", saved.getId()));
            log.info("event=relational_deck_create_success deckId={} authorId={}", saved.getId(), authorId);
            return saved;
        } catch (RuntimeException ex) {
            deckMediaInstrumentation.recordDeckFailure("create", ex.getClass().getSimpleName(), java.util.Map.of("authorId", authorId));
            log.error("event=relational_deck_create_failed authorId={} reason={}", authorId, ex.getMessage(), ex);
            throw exceptionMapper.mapCreateDeckFailure(ex, authorId);
        }
    }

    @Transactional(readOnly = true)
    public Page<Deck> listDecks(Boolean isPublic, UUID authorId, Integer page, Integer size) {
        Pageable pageable = validatedPageable(page, size);

        if (authorId != null) {
            deckCardAccessGuard.ensureCanViewAuthor(authorId);
            return deckRepository.findByAuthorId(authorId, pageable);
        }

        UUID actorId = deckCardAccessGuard.currentUserId().orElse(null);
        if (Boolean.TRUE.equals(isPublic) || actorId == null) {
            return deckRepository.findByIsPublicTrue(pageable);
        }

        if (Boolean.FALSE.equals(isPublic)) {
            return deckRepository.findByAuthorIdAndIsPublic(actorId, false, pageable);
        }

        return deckRepository.findByAuthorId(actorId, pageable);
    }

    @Transactional(readOnly = true)
    public Deck getDeck(UUID deckId) {
        Deck deck = findDeckOrThrow(deckId);
        deckCardAccessGuard.ensureCanReadDeck(deck);
        return deck;
    }

    public Deck updateDeck(UUID deckId, UpdateDeckRequest request) {
        Deck deck = findDeckOrThrow(deckId);
        deckCardAccessGuard.ensureOwnerOrAdmin(
                deck.getAuthor().getId(),
                "update",
                "deck",
                deckId.toString());

        String beforeCover = deck.getCoverImageUrl();

        if (request.name() != null) {
            deck.setName(request.name());
        }
        if (request.description() != null) {
            deck.setDescription(request.description());
        }
        if (request.coverImageUrl() != null) {
            deck.setCoverImageUrl(request.coverImageUrl());
        }
        if (request.tags() != null) {
            deck.setTags(request.tags());
        }
        if (request.isPublic() != null) {
            deck.setIsPublic(request.isPublic());
        }

        try {
            Deck saved = deckRepository.save(deck);
            reconcileMediaReferences(beforeCover, saved.getCoverImageUrl());
            deckMediaInstrumentation.recordDeckOutcome("update", "success", java.util.Map.of("deckId", saved.getId()));
            return saved;
        } catch (RuntimeException ex) {
            deckMediaInstrumentation.recordDeckFailure("update", ex.getClass().getSimpleName(), java.util.Map.of("deckId", deckId));
            throw exceptionMapper.mapCreateDeckFailure(ex, deck.getAuthor().getId());
        }
    }

    public void deleteDeck(UUID deckId) {
        Deck deck = findDeckOrThrow(deckId);
        deckCardAccessGuard.ensureOwnerOrAdmin(
                deck.getAuthor().getId(),
                "delete",
                "deck",
                deckId.toString());

        List<Card> deckCards = cardRepository.findByDeckId(deckId);
        List<UUID> cardIds = deckCards.stream().map(Card::getId).toList();
        if (!cardIds.isEmpty()) {
            cardLearningStateRepository.deleteByCardIdIn(cardIds);
        }

        for (Card card : deckCards) {
            mediaReferenceService.decrementReference(card.getFrontMediaUrl());
            mediaReferenceService.decrementReference(card.getBackMediaUrl());
        }
        mediaReferenceService.decrementReference(deck.getCoverImageUrl());

        cardRepository.deleteByDeckId(deckId);
        deckRepository.delete(deck);
        deckMediaInstrumentation.recordDeckOutcome("delete", "success", java.util.Map.of("deckId", deckId));
    }

    public Card createCard(UUID deckId, CreateCardRequest request) {
        Deck deck = findDeckOrThrow(deckId);
        deckCardAccessGuard.ensureOwnerOrAdmin(
            deck.getAuthor().getId(),
            "create",
            "card",
            deckId.toString());

        if (deck.getAuthor() == null) {
            throw exceptionMapper.missingRelationship("deck.author", deckId.toString());
        }

        try {
            Card card = Card.builder()
                    .deck(deck)
                    .frontText(request.frontText())
                    .frontMediaUrl(request.frontMediaUrl())
                    .backText(request.backText())
                    .backMediaUrl(request.backMediaUrl())
                    .build();

            Card saved = cardRepository.save(card);
            mediaReferenceService.incrementReference(saved.getFrontMediaUrl());
            mediaReferenceService.incrementReference(saved.getBackMediaUrl());
            deckMediaInstrumentation.recordCardOutcome("create", "success", java.util.Map.of("cardId", saved.getId(), "deckId", deckId));
            log.info("event=relational_card_create_success cardId={} deckId={}", saved.getId(), deckId);
            return saved;
        } catch (RuntimeException ex) {
            deckMediaInstrumentation.recordCardFailure("create", ex.getClass().getSimpleName(), java.util.Map.of("deckId", deckId));
            log.error("event=relational_card_create_failed deckId={} reason={}", deckId, ex.getMessage(), ex);
            throw exceptionMapper.mapCreateCardFailure(ex, deckId);
        }
    }

    @Transactional(readOnly = true)
    public Page<Card> searchCardsInDeck(UUID deckId, CardSearchQuery query) {
        Deck deck = findDeckOrThrow(deckId);
        deckCardAccessGuard.ensureCanReadDeck(deck);

        Pageable pageable = validatedPageable(query.page(), query.size());

        Page<Card> result = cardRepository.searchInDeck(
                deckId,
                query.frontText(),
                query.backText(),
                query.vocabulary(),
                pageable);

        deckMediaInstrumentation.recordCardSearch(
                "success",
                java.util.Map.of(
                        "deckId", deckId,
                        "page", pageable.getPageNumber(),
                        "size", pageable.getPageSize(),
                        "resultCount", result.getNumberOfElements()));

        return result;
    }

    public Card updateCard(UUID cardId, UpdateCardRequest request) {
        Card card = findCardOrThrow(cardId);
        Deck deck = Objects.requireNonNull(card.getDeck(), "card.deck must not be null");

        deckCardAccessGuard.ensureOwnerOrAdmin(
                deck.getAuthor().getId(),
                "update",
                "card",
                cardId.toString());

        String beforeFront = card.getFrontMediaUrl();
        String beforeBack = card.getBackMediaUrl();

        if (request.frontText() != null) {
            card.setFrontText(request.frontText());
        }
        if (request.frontMediaUrl() != null) {
            card.setFrontMediaUrl(request.frontMediaUrl());
        }
        if (request.backText() != null) {
            card.setBackText(request.backText());
        }
        if (request.backMediaUrl() != null) {
            card.setBackMediaUrl(request.backMediaUrl());
        }

        try {
            Card saved = cardRepository.save(card);
            reconcileMediaReferences(beforeFront, saved.getFrontMediaUrl());
            reconcileMediaReferences(beforeBack, saved.getBackMediaUrl());
            deckMediaInstrumentation.recordCardOutcome("update", "success", java.util.Map.of("cardId", cardId));
            return saved;
        } catch (RuntimeException ex) {
            deckMediaInstrumentation.recordCardFailure("update", ex.getClass().getSimpleName(), java.util.Map.of("cardId", cardId));
            throw exceptionMapper.mapCreateCardFailure(ex, deck.getId());
        }
    }

    public void deleteCard(UUID cardId) {
        Card card = findCardOrThrow(cardId);
        Deck deck = Objects.requireNonNull(card.getDeck(), "card.deck must not be null");

        deckCardAccessGuard.ensureOwnerOrAdmin(
                deck.getAuthor().getId(),
                "delete",
                "card",
                cardId.toString());

        cardLearningStateRepository.deleteByCardIdIn(List.of(cardId));
        mediaReferenceService.decrementReference(card.getFrontMediaUrl());
        mediaReferenceService.decrementReference(card.getBackMediaUrl());
        cardRepository.delete(card);
        deckMediaInstrumentation.recordCardOutcome("delete", "success", java.util.Map.of("cardId", cardId));
    }

    public CardLearningState upsertLearningState(UpsertLearningStateRequest request) {
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> exceptionMapper.missingRelationship("user", request.userId().toString()));
        Card card = cardRepository.findById(request.cardId())
                .orElseThrow(() -> exceptionMapper.missingRelationship("card", request.cardId().toString()));

        if (card.getDeck() == null) {
            throw exceptionMapper.missingRelationship("card.deck", request.cardId().toString());
        }
        if (card.getDeck().getAuthor() == null) {
            throw exceptionMapper.missingRelationship("card.deck.author", request.cardId().toString());
        }

        try {
            CardLearningState saved = cardLearningStateUpdateService.saveWithSingleRetry(
                    request.userId(),
                    request.cardId(),
                    () -> {
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

                        return cardLearningStateRepository.saveAndFlush(state);
                    });

            log.info("event=relational_learning_state_upsert_success stateId={} userId={} cardId={}",
                    saved.getId(), request.userId(), request.cardId());

            studyActivityLogPublisher.publishLearningStateEvent(
                    request.userId(),
                    request.cardId(),
                    request.ratingGiven(),
                    request.timeSpentMs());

            return saved;
        } catch (RuntimeException ex) {
            log.error("event=relational_learning_state_upsert_failed userId={} cardId={} reason={}",
                    request.userId(), request.cardId(), ex.getMessage(), ex);
            throw exceptionMapper.mapLearningStateFailure(ex, request.userId(), request.cardId());
        }
    }

    public record CreateUserRequest(String email, String passwordHash, Integer dailyLearningLimit) {
    }

    public record CreateDeckRequest(
            String name,
            String description,
            String coverImageUrl,
            Boolean isPublic,
            String tags) {
    }

        public record UpdateDeckRequest(
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

        public record UpdateCardRequest(
            String frontText,
            String frontMediaUrl,
            String backText,
            String backMediaUrl) {
        }

        public record CardSearchQuery(
            String frontText,
            String backText,
            String vocabulary,
            Integer page,
            Integer size) {
        }

    public record UpsertLearningStateRequest(
            UUID userId,
            UUID cardId,
            CardLearningStateType state,
            BigDecimal easeFactor,
            Integer intervalInDays,
            Instant nextReviewDate,
            RatingGiven ratingGiven,
            Long timeSpentMs) {
    }

    private Deck findDeckOrThrow(UUID deckId) {
        return deckRepository.findById(deckId)
                .orElseThrow(() -> exceptionMapper.deckNotFound(deckId));
    }

    private Card findCardOrThrow(UUID cardId) {
        return cardRepository.findById(cardId)
                .orElseThrow(() -> exceptionMapper.cardNotFound(cardId));
    }

    private Pageable validatedPageable(Integer page, Integer size) {
        int resolvedPage = page == null ? 0 : page;
        int resolvedSize = size == null ? 20 : size;

        if (resolvedPage < 0 || resolvedSize < 1 || resolvedSize > 100) {
            throw exceptionMapper.invalidPagination(page, size);
        }

        return PageRequest.of(resolvedPage, resolvedSize);
    }

    private void reconcileMediaReferences(String before, String after) {
        if (before != null && !before.equals(after)) {
            mediaReferenceService.decrementReference(before);
        }
        if (after != null && !after.equals(before)) {
            mediaReferenceService.incrementReference(after);
        }
    }
}
