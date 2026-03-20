package com.khaleo.flashcard.contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.khaleo.flashcard.entity.Deck;
import com.khaleo.flashcard.entity.User;
import com.khaleo.flashcard.entity.enums.UserRole;
import com.khaleo.flashcard.integration.support.IntegrationPersistenceTestBase;
import com.khaleo.flashcard.repository.DeckRepository;
import com.khaleo.flashcard.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@SuppressWarnings("null")
class PublicDeckDiscoveryContractTest extends IntegrationPersistenceTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DeckRepository deckRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void shouldListOnlyPublicDecksForGuest() throws Exception {
        User owner = saveUser("public-owner@example.com", true);
        saveDeck(owner, "Visible Public", true);
        saveDeck(owner, "Hidden Private", false);

        String payload = mockMvc.perform(get("/api/v1/public/decks"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(payload).contains("Visible Public");
        assertThat(payload).doesNotContain("Hidden Private");
    }

    @Test
    void shouldRejectInvalidPaginationForPublicList() throws Exception {
        mockMvc.perform(get("/api/v1/public/decks").param("page", "-1").param("size", "1000"))
                .andExpect(status().isBadRequest());
    }

    private User saveUser(String email, boolean verified) {
        return userRepository.saveAndFlush(User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode("Passw0rd!"))
                .role(UserRole.ROLE_USER)
                .isEmailVerified(verified)
                .build());
    }

    private Deck saveDeck(User author, String name, boolean isPublic) {
        return deckRepository.saveAndFlush(Deck.builder()
                .author(author)
                .name(name)
                .description("desc")
                .isPublic(isPublic)
                .tags("tag")
                .build());
    }
}
