package com.khaleo.flashcard.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.khaleo.flashcard.entity.Deck;
import com.khaleo.flashcard.entity.User;
import com.khaleo.flashcard.integration.support.IntegrationPersistenceTestBase;
import com.khaleo.flashcard.repository.CardRepository;
import com.khaleo.flashcard.repository.DeckRepository;
import com.khaleo.flashcard.repository.UserRepository;
import com.khaleo.flashcard.service.persistence.RelationalPersistenceService;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@SuppressWarnings("null")
class RichCardMigrationIT extends IntegrationPersistenceTestBase {

    @Autowired
    private JdbcTemplate jdbcTemplate;

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
    void shouldExposeRichCardColumnsFromMigration() {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = 'cards'
                  AND column_name IN ('image_url', 'part_of_speech', 'phonetic', 'examples_json', 'version')
                """,
                Integer.class);

        assertThat(count).isEqualTo(5);
    }

    @Test
    void shouldPersistDefaultExamplesArrayForLegacyCompatibleCreatePath() {
        User user = userRepository.save(User.builder()
                .email("rich-card-migration-owner@example.com")
                .passwordHash("hash")
                .build());
        Deck deck = deckRepository.save(Deck.builder()
                .author(user)
                .name("Migration deck")
                .description("desc")
                .isPublic(false)
                .build());

        authenticateAs(user.getId());

        var created = service.createCard(deck.getId(), new RelationalPersistenceService.CreateCardRequest(
                "front legacy",
                null,
                "back legacy",
                null));

        String examplesJson = cardRepository.findById(created.getId())
                .orElseThrow()
                .getExamplesJson();

        assertThat(examplesJson).isEqualTo("[]");
    }

    private void authenticateAs(UUID userId) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId.toString(), null, java.util.Collections.emptyList()));
    }
}
