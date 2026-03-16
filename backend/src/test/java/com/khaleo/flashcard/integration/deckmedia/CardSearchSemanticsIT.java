package com.khaleo.flashcard.integration.deckmedia;

import static org.assertj.core.api.Assertions.assertThat;

import com.khaleo.flashcard.entity.Card;
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
class CardSearchSemanticsIT extends IntegrationPersistenceTestBase {

    @Autowired
    private RelationalPersistenceService service;

    @Autowired
    private UserRepository userRepository;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldMatchFrontAndBackByCaseInsensitiveContains() {
        User owner = saveUser("it-search-owner@example.com", UserRole.ROLE_USER);
        authenticateAs(owner.getId());

        Deck deck = service.createDeck(owner.getId(), new RelationalPersistenceService.CreateDeckRequest("Deck", null, null, true, null));
        service.createCard(deck.getId(), new RelationalPersistenceService.CreateCardRequest("Xin chao", null, "Hello", null));
        service.createCard(deck.getId(), new RelationalPersistenceService.CreateCardRequest("Tam biet", null, "Goodbye", null));

        SecurityContextHolder.clearContext();
        Page<Card> page = service.searchCardsInDeck(deck.getId(), new RelationalPersistenceService.CardSearchQuery("XIN", null, null, 0, 20));

        assertThat(page.getContent()).extracting(Card::getFrontText).containsExactly("Xin chao");
    }

    @Test
    void shouldMatchVocabularyByCaseInsensitiveExact() {
        User owner = saveUser("it-search-owner-2@example.com", UserRole.ROLE_USER);
        authenticateAs(owner.getId());

        Deck deck = service.createDeck(owner.getId(), new RelationalPersistenceService.CreateDeckRequest("Deck", null, null, true, null));
        service.createCard(deck.getId(), new RelationalPersistenceService.CreateCardRequest("apple", null, "tao", null));
        service.createCard(deck.getId(), new RelationalPersistenceService.CreateCardRequest("pineapple", null, "dua", null));

        SecurityContextHolder.clearContext();
        Page<Card> page = service.searchCardsInDeck(deck.getId(), new RelationalPersistenceService.CardSearchQuery(null, null, "APPLE", 0, 20));

        assertThat(page.getContent()).extracting(Card::getFrontText).containsExactly("apple");
    }

    @Test
    void shouldApplyPaginationForSearchResults() {
        User owner = saveUser("it-search-owner-3@example.com", UserRole.ROLE_USER);
        authenticateAs(owner.getId());

        Deck deck = service.createDeck(owner.getId(), new RelationalPersistenceService.CreateDeckRequest("Deck", null, null, true, null));
        service.createCard(deck.getId(), new RelationalPersistenceService.CreateCardRequest("A", null, "1", null));
        service.createCard(deck.getId(), new RelationalPersistenceService.CreateCardRequest("B", null, "2", null));

        SecurityContextHolder.clearContext();
        Page<Card> page = service.searchCardsInDeck(deck.getId(), new RelationalPersistenceService.CardSearchQuery(null, null, null, 0, 1));

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
