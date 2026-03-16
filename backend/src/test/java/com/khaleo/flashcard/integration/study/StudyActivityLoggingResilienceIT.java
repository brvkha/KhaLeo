package com.khaleo.flashcard.integration.study;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import com.khaleo.flashcard.entity.Card;
import com.khaleo.flashcard.entity.Deck;
import com.khaleo.flashcard.entity.User;
import com.khaleo.flashcard.entity.enums.UserRole;
import com.khaleo.flashcard.integration.support.IntegrationPersistenceTestBase;
import com.khaleo.flashcard.model.dynamo.RatingGiven;
import com.khaleo.flashcard.model.study.RateCardRequest;
import com.khaleo.flashcard.model.study.RateCardResponse;
import com.khaleo.flashcard.repository.CardRepository;
import com.khaleo.flashcard.repository.DeckRepository;
import com.khaleo.flashcard.repository.UserRepository;
import com.khaleo.flashcard.repository.dynamo.StudyActivityLogRepository;
import com.khaleo.flashcard.service.activitylog.ActivityLogRetryService;
import com.khaleo.flashcard.service.study.StudyRatingService;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@SuppressWarnings("null")
class StudyActivityLoggingResilienceIT extends IntegrationPersistenceTestBase {

    @Autowired
    private StudyRatingService studyRatingService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DeckRepository deckRepository;

    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private ActivityLogRetryService retryService;

    @MockBean
    private StudyActivityLogRepository studyActivityLogRepository;

    @BeforeEach
    void setUp() {
        retryService.clearDeadLetters();
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldReturnRatingResultEvenWhenAsyncLogFails() {
        doThrow(new RuntimeException("dynamo-down")).when(studyActivityLogRepository).save(any());

        User owner = saveUser("resilience-owner@example.com");
        authenticateAs(owner.getId());
        Deck deck = saveDeck(owner);
        Card card = saveCard(deck);

        RateCardResponse response = studyRatingService.rateCard(card.getId(), new RateCardRequest(RatingGiven.GOOD, 500L));

        assertThat(response.cardId()).isEqualTo(card.getId());
        assertThat(response.state()).isNotNull();
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

    private Card saveCard(Deck deck) {
        return cardRepository.saveAndFlush(Card.builder()
                .deck(deck)
                .frontText("front")
                .backText("back")
                .build());
    }

    private void authenticateAs(UUID userId) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId.toString(), null, java.util.Collections.emptyList()));
    }
}
