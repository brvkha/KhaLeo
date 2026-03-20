package com.khaleo.flashcard.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.khaleo.flashcard.entity.Card;
import com.khaleo.flashcard.entity.Deck;
import com.khaleo.flashcard.entity.User;
import com.khaleo.flashcard.entity.enums.UserRole;
import com.khaleo.flashcard.integration.support.IntegrationPersistenceTestBase;
import com.khaleo.flashcard.repository.UserRepository;
import com.khaleo.flashcard.service.deck.PrivateDeckCrudService;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@SuppressWarnings("null")
class PrivateDeckOwnershipIntegrationTest extends IntegrationPersistenceTestBase {

    @Autowired
    private RelationalPersistenceService relationalPersistenceService;

    @Autowired
    private PrivateDeckCrudService privateDeckCrudService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldAllowOwnerPrivateCrudAndSearch() {
        User owner = saveUser("owner-private-it@example.com", UserRole.ROLE_USER, true);
        authenticate(owner.getId());

        Deck privateDeck = privateDeckCrudService.createPrivateDeck(
                owner.getId(),
                new RelationalPersistenceService.CreateDeckRequest("Owned", "desc", null, false, "tag"));

        Card createdCard = relationalPersistenceService.createCard(
                privateDeck.getId(),
                new RelationalPersistenceService.CreateCardRequest("front", null, "back", null));

        Deck updated = privateDeckCrudService.updatePrivateDeck(
                owner.getId(),
                privateDeck.getId(),
                new RelationalPersistenceService.UpdateDeckRequest("Owned Updated", null, null, false, null));

        assertThat(updated.getName()).isEqualTo("Owned Updated");
        assertThat(privateDeckCrudService.searchCards(
                owner.getId(),
                privateDeck.getId(),
                new RelationalPersistenceService.CardSearchQuery("front", null, null, 0, 20)).getContent())
                        .extracting(Card::getId)
                        .contains(createdCard.getId());

        privateDeckCrudService.deletePrivateDeck(owner.getId(), privateDeck.getId());
        assertThatThrownBy(() -> relationalPersistenceService.getDeck(privateDeck.getId()))
                .isInstanceOf(PersistenceValidationException.class)
                .satisfies(ex -> assertThat(((PersistenceValidationException) ex).getErrorCode())
                        .isEqualTo(PersistenceErrorCode.DECK_NOT_FOUND));
    }

    @Test
    void shouldRejectNonOwnerForPrivateCrud() {
        User owner = saveUser("owner-private-it-2@example.com", UserRole.ROLE_USER, true);
        User intruder = saveUser("intruder-private-it@example.com", UserRole.ROLE_USER, true);

        Deck privateDeck = privateDeckCrudService.createPrivateDeck(
                owner.getId(),
                new RelationalPersistenceService.CreateDeckRequest("Owned", "desc", null, false, "tag"));

        authenticate(intruder.getId());

        assertThatThrownBy(() -> privateDeckCrudService.updatePrivateDeck(
                intruder.getId(),
                privateDeck.getId(),
                new RelationalPersistenceService.UpdateDeckRequest("Hack", null, null, false, null)))
                .isInstanceOf(PersistenceValidationException.class)
                .satisfies(ex -> assertThat(((PersistenceValidationException) ex).getErrorCode())
                        .isEqualTo(PersistenceErrorCode.AUTHORIZATION_DENIED));
    }

    private User saveUser(String email, UserRole role, boolean verified) {
        return userRepository.saveAndFlush(User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode("Passw0rd!"))
                .role(role)
                .isEmailVerified(verified)
                .build());
    }

    private void authenticate(UUID userId) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId.toString(), null, java.util.Collections.emptyList()));
    }
}