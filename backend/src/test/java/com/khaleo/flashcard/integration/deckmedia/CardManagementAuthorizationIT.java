package com.khaleo.flashcard.integration.deckmedia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.khaleo.flashcard.entity.Card;
import com.khaleo.flashcard.entity.Deck;
import com.khaleo.flashcard.entity.User;
import com.khaleo.flashcard.entity.enums.UserRole;
import com.khaleo.flashcard.integration.support.IntegrationPersistenceTestBase;
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
class CardManagementAuthorizationIT extends IntegrationPersistenceTestBase {

    @Autowired
    private RelationalPersistenceService service;

    @Autowired
    private UserRepository userRepository;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldAllowOwnerToUpdateCard() {
        User owner = saveUser("it-card-owner@example.com", UserRole.ROLE_USER);
        authenticateAs(owner.getId());

        Deck deck = service.createDeck(owner.getId(), new RelationalPersistenceService.CreateDeckRequest("Deck", null, null, false, null));
        Card card = service.createCard(deck.getId(), new RelationalPersistenceService.CreateCardRequest("One", null, "Two", null));

        Card updated = service.updateCard(card.getId(), new RelationalPersistenceService.UpdateCardRequest("One Updated", null, null, null));
        assertThat(updated.getFrontText()).isEqualTo("One Updated");
    }

    @Test
    void shouldRejectUpdateForNonOwnerNonAdmin() {
        User owner = saveUser("it-card-owner-2@example.com", UserRole.ROLE_USER);
        User intruder = saveUser("it-card-intruder@example.com", UserRole.ROLE_USER);

        authenticateAs(owner.getId());
        Deck deck = service.createDeck(owner.getId(), new RelationalPersistenceService.CreateDeckRequest("Deck", null, null, false, null));
        Card card = service.createCard(deck.getId(), new RelationalPersistenceService.CreateCardRequest("A", null, "B", null));

        authenticateAs(intruder.getId());
        assertThatThrownBy(() -> service.updateCard(card.getId(), new RelationalPersistenceService.UpdateCardRequest("Hack", null, null, null)))
                .isInstanceOf(PersistenceValidationException.class)
                .satisfies(ex -> assertThat(((PersistenceValidationException) ex).getErrorCode())
                        .isEqualTo(PersistenceErrorCode.AUTHORIZATION_DENIED));
    }

    @Test
    void shouldAllowAdminToDeleteAnotherUsersCard() {
        User owner = saveUser("it-card-owner-3@example.com", UserRole.ROLE_USER);
        User admin = saveUser("it-card-admin@example.com", UserRole.ROLE_ADMIN);

        authenticateAs(owner.getId());
        Deck deck = service.createDeck(owner.getId(), new RelationalPersistenceService.CreateDeckRequest("Deck", null, null, false, null));
        Card card = service.createCard(deck.getId(), new RelationalPersistenceService.CreateCardRequest("A", null, "B", null));

        authenticateAs(admin.getId());
        service.deleteCard(card.getId());

        assertThatThrownBy(() -> service.updateCard(card.getId(), new RelationalPersistenceService.UpdateCardRequest("X", null, null, null)))
                .isInstanceOf(PersistenceValidationException.class)
                .satisfies(ex -> assertThat(((PersistenceValidationException) ex).getErrorCode())
                        .isEqualTo(PersistenceErrorCode.CARD_NOT_FOUND));
    }

    private User saveUser(String email, UserRole role) {
        return userRepository.saveAndFlush(User.builder()
                .email(email)
                .passwordHash("hash")
                .role(role)
                .isEmailVerified(true)
                .build());
    }

    private void authenticateAs(UUID userId) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId.toString(), null, java.util.Collections.emptyList()));
    }
}
