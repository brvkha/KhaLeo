package com.khaleo.flashcard.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.khaleo.flashcard.entity.Card;
import com.khaleo.flashcard.entity.Deck;
import com.khaleo.flashcard.entity.User;
import com.khaleo.flashcard.entity.enums.ImportMergeStatus;
import com.khaleo.flashcard.entity.enums.ReimportConflictResolutionChoice;
import com.khaleo.flashcard.entity.enums.UserRole;
import com.khaleo.flashcard.integration.support.IntegrationPersistenceTestBase;
import com.khaleo.flashcard.repository.CardRepository;
import com.khaleo.flashcard.repository.DeckRepository;
import com.khaleo.flashcard.repository.UserRepository;
import com.khaleo.flashcard.service.importmerge.ConflictResolutionService;
import com.khaleo.flashcard.service.importmerge.PublicDeckImportService;
import com.khaleo.flashcard.service.importmerge.ReimportMergeService;
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
class ReimportMergeConflictResolutionIntegrationTest extends IntegrationPersistenceTestBase {

    @Autowired
    private PublicDeckImportService publicDeckImportService;

    @Autowired
    private ReimportMergeService reimportMergeService;

    @Autowired
    private ConflictResolutionService conflictResolutionService;

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
    void shouldCreateConflictAndApplyCloudChoice() {
        User owner = saveUser("conflict-owner@example.com", true);
        User actor = saveUser("conflict-actor@example.com", true);

        Deck source = saveDeck(owner, "Cloud Name", "desc", true);
        saveCard(source, "front", "back");

        authenticate(actor.getId());
        var imported = publicDeckImportService.importPublicDeck(actor.getId(), source.getId());

        Deck target = deckRepository.findById(imported.targetPrivateDeckId()).orElseThrow();
        target.setName("Local Name");
        deckRepository.saveAndFlush(target);

        source.setName("Cloud Name Updated");
        deckRepository.saveAndFlush(source);

        var reimport = reimportMergeService.reimport(actor.getId(), imported.importLinkId());
        assertThat(reimport.status()).isEqualTo(ImportMergeStatus.CONFLICT_REQUIRED);
        assertThat(reimport.conflictsCount()).isGreaterThan(0);

        var conflicts = conflictResolutionService.listUnresolved(actor.getId(), imported.importLinkId());
        var first = conflicts.get(0);

        var resolved = conflictResolutionService.resolve(
                actor.getId(),
                imported.importLinkId(),
                first.getId(),
                ReimportConflictResolutionChoice.CLOUD);

        assertThat(resolved.remainingConflicts()).isZero();
        Deck targetReloaded = deckRepository.findById(imported.targetPrivateDeckId()).orElseThrow();
        assertThat(targetReloaded.getName()).isEqualTo("Cloud Name Updated");
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
