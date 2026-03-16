package com.khaleo.flashcard.contract.deckmedia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.khaleo.flashcard.entity.Card;
import com.khaleo.flashcard.entity.Deck;
import com.khaleo.flashcard.entity.User;
import com.khaleo.flashcard.entity.enums.UserRole;
import com.khaleo.flashcard.integration.support.IntegrationPersistenceTestBase;
import com.khaleo.flashcard.repository.CardRepository;
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
class CardManagementSearchContractTest extends IntegrationPersistenceTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DeckRepository deckRepository;

    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void shouldCreateAndDeleteCardWhenOwnerAuthenticated() throws Exception {
        User owner = createUser("contract-card-owner@example.com", UserRole.ROLE_USER);
        Deck deck = createDeck(owner, "Vocabulary", true);

        String createBody = objectMapper.writeValueAsString(Map.of(
                "frontText", "Bonjour",
                "backText", "Hello"));

        String createResponse = mockMvc.perform(post("/api/v1/decks/{deckId}/cards", deck.getId())
                        .header("Authorization", bearerFor(owner))
                        .contentType("application/json")
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(createResponse).contains("Bonjour");

        Card card = cardRepository.findByDeckId(deck.getId()).get(0);

        mockMvc.perform(delete("/api/v1/cards/{id}", card.getId())
                        .header("Authorization", bearerFor(owner)))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldSearchCardsInDeckWithExpectedSemantics() throws Exception {
        User owner = createUser("contract-card-search@example.com", UserRole.ROLE_USER);
        Deck deck = createDeck(owner, "Language", true);

        createCard(deck, "Xin chao", "Hello");
        createCard(deck, "Tam biet", "Goodbye");

        String response = mockMvc.perform(get("/api/v1/decks/{deckId}/cards/search", deck.getId())
                        .queryParam("frontText", "xin"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(response).contains("Xin chao");
        assertThat(response).doesNotContain("Tam biet");
    }

    @Test
    void shouldRejectCardMutationForNonOwnerNonAdmin() throws Exception {
        User owner = createUser("contract-card-owner-2@example.com", UserRole.ROLE_USER);
        User intruder = createUser("contract-card-intruder@example.com", UserRole.ROLE_USER);
        Deck deck = createDeck(owner, "Deck", false);
        Card card = createCard(deck, "A", "B");

        String body = objectMapper.writeValueAsString(Map.of("frontText", "Hacked"));

        mockMvc.perform(put("/api/v1/cards/{id}", card.getId())
                        .header("Authorization", bearerFor(intruder))
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isForbidden());
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
        return deckRepository.saveAndFlush(Deck.builder()
                .author(author)
                .name(name)
                .description("desc")
                .isPublic(isPublic)
                .tags("tag")
                .build());
    }

    private Card createCard(Deck deck, String frontText, String backText) {
        return cardRepository.saveAndFlush(Card.builder()
                .deck(deck)
                .frontText(frontText)
                .backText(backText)
                .build());
    }

    private String bearerFor(User user) {
        return "Bearer " + jwtTokenService.createAccessToken(user.getId().toString(), Map.of("role", user.getRole().name()));
    }
}
