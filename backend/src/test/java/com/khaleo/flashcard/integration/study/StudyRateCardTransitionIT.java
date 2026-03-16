package com.khaleo.flashcard.integration.study;

import static org.assertj.core.api.Assertions.assertThat;

import com.khaleo.flashcard.entity.Card;
import com.khaleo.flashcard.entity.CardLearningState;
import com.khaleo.flashcard.entity.Deck;
import com.khaleo.flashcard.entity.User;
import com.khaleo.flashcard.entity.enums.CardLearningStateType;
import com.khaleo.flashcard.entity.enums.UserRole;
import com.khaleo.flashcard.integration.support.IntegrationPersistenceTestBase;
import com.khaleo.flashcard.model.dynamo.RatingGiven;
import com.khaleo.flashcard.model.study.RateCardRequest;
import com.khaleo.flashcard.model.study.RateCardResponse;
import com.khaleo.flashcard.repository.CardLearningStateRepository;
import com.khaleo.flashcard.repository.CardRepository;
import com.khaleo.flashcard.repository.DeckRepository;
import com.khaleo.flashcard.repository.UserRepository;
import com.khaleo.flashcard.service.study.StudyRatingService;
import java.math.BigDecimal;
import java.time.Instant;
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
class StudyRateCardTransitionIT extends IntegrationPersistenceTestBase {

    @Autowired
    private StudyRatingService studyRatingService;

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
    void shouldApplyNewToLearningToMasteredFlow() {
        User owner = saveUser("rate-it-owner@example.com");
        authenticateAs(owner.getId());
        Deck deck = saveDeck(owner);
        Card card = saveCard(deck, "front", "back");

        RateCardResponse first = studyRatingService.rateCard(card.getId(), new RateCardRequest(RatingGiven.GOOD, 1000L));
        assertThat(first.state()).isEqualTo(CardLearningStateType.LEARNING);

        RateCardResponse second = studyRatingService.rateCard(card.getId(), new RateCardRequest(RatingGiven.GOOD, 800L));
        assertThat(second.state()).isEqualTo(CardLearningStateType.MASTERED);
        assertThat(second.newInterval()).isEqualTo(1);
    }

    @Test
    void shouldApplyAgainWithEaseFloor() {
        User owner = saveUser("rate-it-owner-2@example.com");
        authenticateAs(owner.getId());
        Deck deck = saveDeck(owner);
        Card card = saveCard(deck, "front", "back");

        cardLearningStateRepository.saveAndFlush(CardLearningState.builder()
                .user(owner)
                .card(card)
                .state(CardLearningStateType.REVIEW)
                .easeFactor(BigDecimal.valueOf(1.35))
                .intervalInDays(6)
                .nextReviewDate(Instant.now().minusSeconds(5))
                .learningStepGoodCount(1)
                .build());

        RateCardResponse response = studyRatingService.rateCard(card.getId(), new RateCardRequest(RatingGiven.AGAIN, 700L));

        assertThat(response.state()).isEqualTo(CardLearningStateType.LEARNING);
        assertThat(response.newInterval()).isZero();
        assertThat(response.newEaseFactor()).isEqualByComparingTo("1.3");
    }

    private User saveUser(String email) {
        return userRepository.saveAndFlush(User.builder()
                .email(email)
                .passwordHash("hash")
                .role(UserRole.ROLE_USER)
                .isEmailVerified(true)
                .dailyLearningLimit(20)
                .build());
    }

    private Deck saveDeck(User owner) {
        return deckRepository.saveAndFlush(Deck.builder()
                .author(owner)
                .name("Deck")
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

    private void authenticateAs(UUID userId) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId.toString(), null, java.util.Collections.emptyList()));
    }
}
