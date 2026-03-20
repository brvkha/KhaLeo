package com.khaleo.flashcard.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.khaleo.flashcard.entity.Card;
import com.khaleo.flashcard.entity.Deck;
import com.khaleo.flashcard.entity.User;
import com.khaleo.flashcard.entity.enums.UserRole;
import com.khaleo.flashcard.integration.support.IntegrationPersistenceTestBase;
import com.khaleo.flashcard.repository.CardRepository;
import com.khaleo.flashcard.repository.DeckRepository;
import com.khaleo.flashcard.repository.UserRepository;
import com.khaleo.flashcard.service.importmerge.PublicDeckImportService;
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
class PublicDeckImportIntegrationTest extends IntegrationPersistenceTestBase {

    @Autowired
    private PublicDeckImportService publicDeckImportService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DeckRepository deckRepository;

    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldCopyMetadataAndCardsWithoutMutatingSourceDeck() {
        User owner = saveUser("import-owner@example.com", true);
        User actor = saveUser("import-actor@example.com", true);

        Deck source = saveDeck(owner, "Public Source", "source desc", true);
        saveCard(source, "front-1", "back-1");
        saveCard(source, "front-2", "back-2");

        authenticate(actor.getId());
        var result = publicDeckImportService.importPublicDeck(actor.getId(), source.getId());

        Deck target = deckRepository.findById(result.targetPrivateDeckId()).orElseThrow();
        assertThat(target.getAuthor().getId()).isEqualTo(actor.getId());
        assertThat(target.getIsPublic()).isFalse();
        assertThat(target.getName()).isEqualTo(source.getName());
        assertThat(cardRepository.findByDeckId(target.getId())).hasSize(2);

        Deck sourceReloaded = deckRepository.findById(source.getId()).orElseThrow();
        assertThat(sourceReloaded.getName()).isEqualTo("Public Source");
        assertThat(cardRepository.findByDeckId(source.getId())).hasSize(2);
    }

    private User saveUser(String email, boolean verified) {
        return userRepository.saveAndFlush(User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode("Passw0rd!"))
                .role(UserRole.ROLE_USER)
                .isEmailVerified(verified)
                .build());
    }

    private Deck saveDeck(User author, String name, String description, boolean isPublic) {
        return deckRepository.saveAndFlush(Deck.builder()
                .author(author)
                .name(name)
                .description(description)
                .isPublic(isPublic)
                .tags("tag")
                .build());
    }

    private Card saveCard(Deck deck, String front, String back) {
        return cardRepository.saveAndFlush(Card.builder()
                .deck(deck)
                .frontText(front)
                .backText(back)
                .build());
    }

    private void authenticate(UUID userId) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId.toString(), null, java.util.Collections.emptyList()));
    }
}
