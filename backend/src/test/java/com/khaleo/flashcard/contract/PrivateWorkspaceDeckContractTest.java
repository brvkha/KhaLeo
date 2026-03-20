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
import com.khaleo.flashcard.service.auth.JwtTokenService;
import java.util.Map;
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
class PrivateWorkspaceDeckContractTest extends IntegrationPersistenceTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DeckRepository deckRepository;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void shouldListOnlyActorsPrivateDecks() throws Exception {
        User actor = saveUser("private-actor@example.com", UserRole.ROLE_USER, true);
        User other = saveUser("private-other@example.com", UserRole.ROLE_USER, true);

        saveDeck(actor, "Actor Private", false);
        saveDeck(actor, "Actor Public", true);
        saveDeck(other, "Other Private", false);

        String response = mockMvc.perform(get("/api/v1/private/decks")
                        .header("Authorization", bearerFor(actor)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(response).contains("Actor Private");
        assertThat(response).doesNotContain("Actor Public");
        assertThat(response).doesNotContain("Other Private");
    }

    private User saveUser(String email, UserRole role, boolean verified) {
        return userRepository.saveAndFlush(User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode("Passw0rd!"))
                .role(role)
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

    private String bearerFor(User user) {
        return "Bearer " + jwtTokenService.createAccessToken(
                user.getId().toString(),
                Map.of("role", user.getRole().name()));
    }
}
