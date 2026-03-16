package com.khaleo.flashcard.contract.deckmedia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.khaleo.flashcard.controller.deck.dto.CreateDeckRequest;
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
class DeckManagementContractTest extends IntegrationPersistenceTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DeckRepository deckRepository;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void shouldCreateDeckWhenAuthenticated() throws Exception {
        User owner = createUser("contract-owner@example.com", UserRole.ROLE_USER);
        String token = bearerFor(owner);

        CreateDeckRequest request = new CreateDeckRequest("Biology", "Cells", null, true, "bio");

        String response = mockMvc.perform(post("/api/v1/decks")
                        .header("Authorization", token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(response).contains("Biology");
        assertThat(deckRepository.findByAuthorId(owner.getId())).hasSize(1);
    }

    @Test
    void shouldListOnlyPublicDecksWhenAnonymous() throws Exception {
        User owner = createUser("contract-public@example.com", UserRole.ROLE_USER);
        createDeck(owner, "Public Deck", true);
        createDeck(owner, "Private Deck", false);

        String response = mockMvc.perform(get("/api/v1/decks")
                        .queryParam("isPublic", "true"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(response).contains("Public Deck");
        assertThat(response).doesNotContain("Private Deck");
    }

    @Test
    void shouldRejectUpdateWhenRequesterIsNotOwnerOrAdmin() throws Exception {
        User owner = createUser("contract-owner-2@example.com", UserRole.ROLE_USER);
        User intruder = createUser("contract-intruder@example.com", UserRole.ROLE_USER);
        Deck deck = createDeck(owner, "Deck X", true);

        String intruderToken = bearerFor(intruder);

        String body = objectMapper.writeValueAsString(Map.of("name", "Hacked Name"));

        mockMvc.perform(put("/api/v1/decks/{id}", deck.getId())
                        .header("Authorization", intruderToken)
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldDeleteDeckWhenOwner() throws Exception {
        User owner = createUser("contract-owner-3@example.com", UserRole.ROLE_USER);
        Deck deck = createDeck(owner, "Deck Delete", false);

        mockMvc.perform(delete("/api/v1/decks/{id}", deck.getId())
                        .header("Authorization", bearerFor(owner)))
                .andExpect(status().isNoContent());

        assertThat(deckRepository.findById(deck.getId())).isEmpty();
    }

    private User createUser(String email, UserRole role) {
        User user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode("Passw0rd!"))
                .role(role)
                .isEmailVerified(true)
                .build();
        return userRepository.saveAndFlush(user);
    }

    private Deck createDeck(User author, String name, boolean isPublic) {
        Deck deck = Deck.builder()
                .author(author)
                .name(name)
                .description("desc")
                .isPublic(isPublic)
                .tags("tag")
                .build();
        return deckRepository.saveAndFlush(deck);
    }

    private String bearerFor(User user) {
        return "Bearer " + jwtTokenService.createAccessToken(
                user.getId().toString(),
                Map.of("role", user.getRole().name()));
    }
}
