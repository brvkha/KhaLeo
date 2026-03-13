package com.khaleo.flashcard.integration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.khaleo.flashcard.entity.Card;
import com.khaleo.flashcard.entity.CardLearningState;
import com.khaleo.flashcard.entity.Deck;
import com.khaleo.flashcard.entity.User;
import com.khaleo.flashcard.integration.support.IntegrationPersistenceTestBase;
import com.khaleo.flashcard.repository.CardLearningStateRepository;
import com.khaleo.flashcard.repository.CardRepository;
import com.khaleo.flashcard.repository.DeckRepository;
import com.khaleo.flashcard.repository.UserRepository;
import com.khaleo.flashcard.service.persistence.RelationalPersistenceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class CardLearningStateUniquenessIT extends IntegrationPersistenceTestBase {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DeckRepository deckRepository;

    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private CardLearningStateRepository stateRepository;

    @Autowired
    private RelationalPersistenceService service;

    @Test
    void shouldRejectDuplicateActiveLearningStateForSameUserAndCard() {
        User user = userRepository.save(User.builder()
                .email("state-user@example.com")
                .passwordHash("hash")
                .build());

        Deck deck = deckRepository.save(Deck.builder()
                .author(user)
                .name("Deck")
                .description("D")
                .isPublic(false)
                .build());

        Card card = cardRepository.save(Card.builder()
                .deck(deck)
                .frontText("front")
                .backText("back")
                .build());

        service.upsertLearningState(new RelationalPersistenceService.UpsertLearningStateRequest(
                user.getId(),
                card.getId(),
                null,
                null,
                null,
                null));

        CardLearningState duplicate = CardLearningState.builder()
                .user(user)
                .card(card)
                .build();

        assertThatThrownBy(() -> stateRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
