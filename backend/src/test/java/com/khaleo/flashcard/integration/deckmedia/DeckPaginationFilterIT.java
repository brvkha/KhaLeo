package com.khaleo.flashcard.integration.deckmedia;

import static org.assertj.core.api.Assertions.assertThat;

import com.khaleo.flashcard.entity.Deck;
import com.khaleo.flashcard.entity.User;
import com.khaleo.flashcard.entity.enums.UserRole;
import com.khaleo.flashcard.integration.support.IntegrationPersistenceTestBase;
import com.khaleo.flashcard.repository.UserRepository;
import com.khaleo.flashcard.service.persistence.RelationalPersistenceService;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@SuppressWarnings("null")
class DeckPaginationFilterIT extends IntegrationPersistenceTestBase {

    @Autowired
    private RelationalPersistenceService service;

    @Autowired
    private UserRepository userRepository;

    @AfterEach
    void cleanupAuth() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldReturnOnlyPublicDecksForAnonymousListing() {
        User owner = saveUser("it-public-owner@example.com", UserRole.ROLE_USER);
        service.createDeck(owner.getId(), new RelationalPersistenceService.CreateDeckRequest("Public A", null, null, true, null));
        service.createDeck(owner.getId(), new RelationalPersistenceService.CreateDeckRequest("Private A", null, null, false, null));

        Page<Deck> page = service.listDecks(true, null, 0, 20);

        assertThat(page.getContent()).extracting(Deck::getName).contains("Public A");
        assertThat(page.getContent()).extracting(Deck::getName).doesNotContain("Private A");
    }

    @Test
    void shouldReturnAuthorDecksWhenOwnerRequestsByAuthorId() {
        User owner = saveUser("it-author-owner@example.com", UserRole.ROLE_USER);
        service.createDeck(owner.getId(), new RelationalPersistenceService.CreateDeckRequest("Owner One", null, null, false, null));
        service.createDeck(owner.getId(), new RelationalPersistenceService.CreateDeckRequest("Owner Two", null, null, true, null));

        authenticateAs(owner.getId());
        Page<Deck> page = service.listDecks(null, owner.getId(), 0, 20);

        assertThat(page.getTotalElements()).isEqualTo(2);
    }

    @Test
    void shouldApplyPaginationBounds() {
        User owner = saveUser("it-page-owner@example.com", UserRole.ROLE_USER);
        service.createDeck(owner.getId(), new RelationalPersistenceService.CreateDeckRequest("P1", null, null, true, null));
        service.createDeck(owner.getId(), new RelationalPersistenceService.CreateDeckRequest("P2", null, null, true, null));

        Page<Deck> page = service.listDecks(true, null, 0, 1);
        assertThat(page.getSize()).isEqualTo(1);
        assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(2);
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
