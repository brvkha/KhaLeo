package com.khaleo.flashcard.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.khaleo.flashcard.entity.Deck;
import com.khaleo.flashcard.entity.User;
import com.khaleo.flashcard.integration.support.IntegrationPersistenceTestBase;
import com.khaleo.flashcard.repository.DeckRepository;
import com.khaleo.flashcard.repository.UserRepository;
import com.khaleo.flashcard.service.persistence.PersistenceValidationException;
import com.khaleo.flashcard.service.persistence.PersistenceValidationException.PersistenceErrorCode;
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
class CardContentValidationIT extends IntegrationPersistenceTestBase {

    @Autowired
    private RelationalPersistenceService service;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DeckRepository deckRepository;

        @AfterEach
        void clearContext() {
                SecurityContextHolder.clearContext();
        }

    @Test
    void shouldRejectCardWhenFrontOrBackHasNoContent() {
        User user = userRepository.save(User.builder()
                .email("card-content-owner@example.com")
                .passwordHash("hash")
                .build());

        Deck deck = deckRepository.save(Deck.builder()
                .author(user)
                .name("Content rules deck")
                .description("Validation")
                .isPublic(false)
                .build());

        authenticateAs(user.getId());

        assertThatThrownBy(() -> service.createCard(deck.getId(), new RelationalPersistenceService.CreateCardRequest(
                "   ",
                null,
                null,
                "  ")))
                .isInstanceOf(PersistenceValidationException.class)
                .satisfies(ex -> assertThat(((PersistenceValidationException) ex).getErrorCode())
                        .isEqualTo(PersistenceErrorCode.INVALID_CARD_CONTENT));
    }

        private void authenticateAs(UUID userId) {
                SecurityContextHolder.getContext().setAuthentication(
                                new UsernamePasswordAuthenticationToken(userId.toString(), null, java.util.Collections.emptyList()));
        }
}
