package com.khaleo.flashcard.integration.deckmedia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.khaleo.flashcard.entity.Deck;
import com.khaleo.flashcard.entity.User;
import com.khaleo.flashcard.entity.enums.UserRole;
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
class DeckManagementAuthorizationIT extends IntegrationPersistenceTestBase {

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
    void shouldAllowOwnerToUpdateDeck() {
        User owner = saveUser("it-owner@example.com", UserRole.ROLE_USER);
        Deck deck = service.createDeck(owner.getId(), new RelationalPersistenceService.CreateDeckRequest(
                "Original",
                "desc",
                null,
                false,
                "tag"));

        authenticateAs(owner.getId());
        Deck updated = service.updateDeck(deck.getId(), new RelationalPersistenceService.UpdateDeckRequest(
                "Updated",
                null,
                null,
                null,
                null));

        assertThat(updated.getName()).isEqualTo("Updated");
    }

    @Test
    void shouldRejectUpdateForNonOwnerNonAdmin() {
        User owner = saveUser("it-owner-2@example.com", UserRole.ROLE_USER);
        User intruder = saveUser("it-intruder@example.com", UserRole.ROLE_USER);

        Deck deck = service.createDeck(owner.getId(), new RelationalPersistenceService.CreateDeckRequest(
                "Secured",
                "desc",
                null,
                false,
                null));

        authenticateAs(intruder.getId());

        assertThatThrownBy(() -> service.updateDeck(deck.getId(), new RelationalPersistenceService.UpdateDeckRequest(
                "Bad",
                null,
                null,
                null,
                null)))
                .isInstanceOf(PersistenceValidationException.class)
                .satisfies(ex -> assertThat(((PersistenceValidationException) ex).getErrorCode())
                        .isEqualTo(PersistenceErrorCode.AUTHORIZATION_DENIED));
    }

    @Test
    void shouldAllowAdminToDeleteAnotherUsersDeck() {
        User owner = saveUser("it-owner-3@example.com", UserRole.ROLE_USER);
        User admin = saveUser("it-admin@example.com", UserRole.ROLE_ADMIN);

        Deck deck = service.createDeck(owner.getId(), new RelationalPersistenceService.CreateDeckRequest(
                "To Delete",
                "desc",
                null,
                false,
                null));

        authenticateAs(admin.getId());
        service.deleteDeck(deck.getId());

        assertThat(deckRepository.findById(deck.getId())).isEmpty();
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
