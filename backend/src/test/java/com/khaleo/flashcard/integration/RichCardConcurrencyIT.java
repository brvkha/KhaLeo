package com.khaleo.flashcard.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.khaleo.flashcard.entity.Card;
import com.khaleo.flashcard.entity.Deck;
import com.khaleo.flashcard.entity.User;
import com.khaleo.flashcard.integration.support.IntegrationPersistenceTestBase;
import com.khaleo.flashcard.repository.CardRepository;
import com.khaleo.flashcard.repository.DeckRepository;
import com.khaleo.flashcard.repository.UserRepository;
import com.khaleo.flashcard.service.persistence.PersistenceValidationException;
import com.khaleo.flashcard.service.persistence.RelationalPersistenceService;
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
class RichCardConcurrencyIT extends IntegrationPersistenceTestBase {

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
    void shouldRejectUpdateWhenVersionIsStale() {
        User user = userRepository.save(User.builder()
                .email("rich-card-concurrency-owner@example.com")
                .passwordHash("hash")
                .build());
        Deck deck = deckRepository.save(Deck.builder()
                .author(user)
                .name("Concurrency deck")
                .description("desc")
                .isPublic(false)
                .build());
        authenticateAs(user.getId());

        Card card = cardRepository.saveAndFlush(Card.builder()
                .deck(deck)
                .frontText("Old")
                .backText("Old answer")
                .examplesJson("[]")
                .build());

        assertThatThrownBy(() -> service.updateCard(
                card.getId(),
                new RelationalPersistenceService.UpdateCardRequest(
                        "New",
                        "New answer",
                        null,
                        null,
                        null,
                        List.of(),
                        card.getVersion() + 1)))
                .isInstanceOf(PersistenceValidationException.class)
                .satisfies(ex -> assertThat(((PersistenceValidationException) ex).getErrorCode())
                        .isEqualTo(PersistenceValidationException.PersistenceErrorCode.OPTIMISTIC_LOCK_CONFLICT));
    }

    private void authenticateAs(UUID userId) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId.toString(), null, java.util.Collections.emptyList()));
    }
}
