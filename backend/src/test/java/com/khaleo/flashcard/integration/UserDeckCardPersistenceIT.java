package com.khaleo.flashcard.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.khaleo.flashcard.entity.Card;
import com.khaleo.flashcard.entity.Deck;
import com.khaleo.flashcard.entity.User;
import com.khaleo.flashcard.integration.support.IntegrationPersistenceTestBase;
import com.khaleo.flashcard.repository.CardRepository;
import com.khaleo.flashcard.repository.DeckRepository;
import com.khaleo.flashcard.repository.UserRepository;
import com.khaleo.flashcard.service.persistence.RelationalPersistenceService;
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
class UserDeckCardPersistenceIT extends IntegrationPersistenceTestBase {

    @Autowired
    private RelationalPersistenceService service;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DeckRepository deckRepository;

    @Autowired
    private CardRepository cardRepository;

        @AfterEach
        void clearContext() {
                SecurityContextHolder.clearContext();
        }

    @Test
    void shouldPersistUserDeckAndCardWithValidRelationships() {
        User user = service.createUser(new RelationalPersistenceService.CreateUserRequest(
                "learner@example.com",
                "hashed-password",
                null));

        Deck deck = service.createDeck(user.getId(), new RelationalPersistenceService.CreateDeckRequest(
                "Core Deck",
                "Foundational cards",
                null,
                false,
                "foundational,core"));

        authenticateAs(user.getId());

        Card card = service.createCard(deck.getId(), new RelationalPersistenceService.CreateCardRequest(
                "Front",
                null,
                "Back",
                null));

        assertThat(user.getId()).isNotNull();
        assertThat(deck.getId()).isNotNull();
        assertThat(card.getId()).isNotNull();

        assertThat(deckRepository.findById(deck.getId())).isPresent();
        assertThat(cardRepository.findById(card.getId())).isPresent();
        assertThat(userRepository.findByEmail("learner@example.com")).isPresent();

        Card storedCard = cardRepository.findById(card.getId()).orElseThrow();
        assertThat(storedCard.getDeck().getId()).isEqualTo(deck.getId());
    }

        private void authenticateAs(UUID userId) {
                SecurityContextHolder.getContext().setAuthentication(
                                new UsernamePasswordAuthenticationToken(userId.toString(), null, java.util.Collections.emptyList()));
        }
}
