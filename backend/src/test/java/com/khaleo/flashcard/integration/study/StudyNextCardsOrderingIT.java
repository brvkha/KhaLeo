package com.khaleo.flashcard.integration.study;

import static org.assertj.core.api.Assertions.assertThat;

import com.khaleo.flashcard.entity.Card;
import com.khaleo.flashcard.entity.CardLearningState;
import com.khaleo.flashcard.entity.Deck;
import com.khaleo.flashcard.entity.User;
import com.khaleo.flashcard.entity.enums.CardLearningStateType;
import com.khaleo.flashcard.entity.enums.UserRole;
import com.khaleo.flashcard.integration.support.IntegrationPersistenceTestBase;
import com.khaleo.flashcard.model.study.NextCardsPageResponse;
import com.khaleo.flashcard.model.study.NextCardsRequest;
import com.khaleo.flashcard.repository.CardLearningStateRepository;
import com.khaleo.flashcard.repository.CardRepository;
import com.khaleo.flashcard.repository.DeckRepository;
import com.khaleo.flashcard.repository.UserRepository;
import com.khaleo.flashcard.service.study.NextCardsService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@SuppressWarnings("null")
class StudyNextCardsOrderingIT extends IntegrationPersistenceTestBase {

    @Autowired
    private NextCardsService nextCardsService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DeckRepository deckRepository;

    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private CardLearningStateRepository cardLearningStateRepository;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldPrioritizeLearningThenReviewThenNewAndPaginate() {
        User owner = saveUser("study-it-owner@example.com", 3);
        authenticateAs(owner.getId());

        Deck deck = saveDeck(owner);
        Card learningCard = saveCard(deck, "L-front", "L-back");
        Card reviewCard = saveCard(deck, "R-front", "R-back");
        Card newCard = saveCard(deck, "N-front", "N-back");

        saveState(owner, learningCard, CardLearningStateType.LEARNING, Instant.now().minusSeconds(120));
        saveState(owner, reviewCard, CardLearningStateType.REVIEW, Instant.now().minusSeconds(60));

        NextCardsPageResponse page1 = nextCardsService.getNextCards(deck.getId(), new NextCardsRequest(2, null));
        assertThat(page1.items()).hasSize(2);
        assertThat(page1.items().get(0).cardId()).isEqualTo(learningCard.getId());
        assertThat(page1.items().get(1).cardId()).isEqualTo(reviewCard.getId());
        assertThat(page1.hasMore()).isTrue();

        NextCardsPageResponse page2 = nextCardsService.getNextCards(
                deck.getId(),
                new NextCardsRequest(2, page1.nextContinuationToken()));
        assertThat(page2.items()).hasSize(1);
        assertThat(page2.items().get(0).cardId()).isEqualTo(newCard.getId());
        assertThat(page2.hasMore()).isFalse();
    }

    @Test
    void shouldExcludeNewCardsWhenDailyLimitAlreadyReached() {
        User owner = saveUser("study-it-owner-2@example.com", 1);
        authenticateAs(owner.getId());

        Deck deck = saveDeck(owner);
        Card alreadyStudied = saveCard(deck, "old-front", "old-back");
        Card unseen = saveCard(deck, "new-front", "new-back");

        saveState(owner, alreadyStudied, CardLearningStateType.REVIEW, Instant.now().minusSeconds(10));

        NextCardsPageResponse response = nextCardsService.getNextCards(deck.getId(), new NextCardsRequest(10, null));
        List<UUID> cardIds = response.items().stream().map(item -> item.cardId()).toList();

        assertThat(cardIds).contains(alreadyStudied.getId());
        assertThat(cardIds).doesNotContain(unseen.getId());
    }

    private User saveUser(String email, int dailyLimit) {
        return userRepository.saveAndFlush(User.builder()
                .email(email)
                .passwordHash("hash")
                .role(UserRole.ROLE_USER)
                .isEmailVerified(true)
                .dailyLearningLimit(dailyLimit)
                .build());
    }

    private Deck saveDeck(User owner) {
        return deckRepository.saveAndFlush(Deck.builder()
                .author(owner)
                .name("Study Deck")
                .description("desc")
                .isPublic(false)
                .tags("tag")
                .build());
    }

    private Card saveCard(Deck deck, String front, String back) {
        return cardRepository.saveAndFlush(Card.builder()
                .deck(deck)
                .frontText(front)
                .backText(back)
                .build());
    }

    private void saveState(User user, Card card, CardLearningStateType type, Instant dueAt) {
        cardLearningStateRepository.saveAndFlush(CardLearningState.builder()
                .user(user)
                .card(card)
                .state(type)
                .easeFactor(BigDecimal.valueOf(2.5))
                .intervalInDays(1)
                .nextReviewDate(dueAt)
                .learningStepGoodCount(1)
                .build());
    }

    private void authenticateAs(UUID userId) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId.toString(), null, java.util.Collections.emptyList()));
    }
}
