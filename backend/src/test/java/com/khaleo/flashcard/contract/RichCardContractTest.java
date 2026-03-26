package com.khaleo.flashcard.contract;

import static org.assertj.core.api.Assertions.assertThat;
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
import java.util.List;
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
class RichCardContractTest extends IntegrationPersistenceTestBase {

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
    void shouldCreateRichCardWithDomainAgnosticPayload() throws Exception {
        User owner = saveUser("rich-card-contract-owner@example.com");
        Deck deck = saveDeck(owner);

        String body = objectMapper.writeValueAsString(Map.of(
                "term", "Abstraction",
                "answer", "Generalized representation",
                "imageUrl", "https://images.unsplash.com/photo-1",
                "partOfSpeech", "noun",
                "phonetic", "ab-strak-shun",
                "examples", List.of("Example one", "Example two")));

        String response = mockMvc.perform(post("/api/v1/decks/{deckId}/cards", deck.getId())
                        .header("Authorization", bearerFor(owner))
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(response).contains("term");
        assertThat(response).contains("answer");
        assertThat(response).contains("examples");
        assertThat(response).contains("version");
    }

    @Test
    void shouldRejectRichCardWhenExampleIsWhitespaceOnly() throws Exception {
        User owner = saveUser("rich-card-contract-owner-2@example.com");
        Deck deck = saveDeck(owner);

        String body = objectMapper.writeValueAsString(Map.of(
                "term", "Abstraction",
                "answer", "Generalized representation",
                "examples", List.of("   ")));

        mockMvc.perform(post("/api/v1/decks/{deckId}/cards", deck.getId())
                        .header("Authorization", bearerFor(owner))
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void shouldReturnConflictOnStaleVersionUpdate() throws Exception {
        User owner = saveUser("rich-card-contract-owner-3@example.com");
        Deck deck = saveDeck(owner);
        Card card = cardRepository.saveAndFlush(Card.builder()
                .deck(deck)
                .frontText("Old term")
                .backText("Old answer")
                .examplesJson("[]")
                .build());

        String body = objectMapper.writeValueAsString(Map.of(
                "term", "New term",
                "answer", "New answer",
                "examples", List.of(),
                "version", 99));

        mockMvc.perform(put("/api/v1/cards/{id}", card.getId())
                        .header("Authorization", bearerFor(owner))
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isConflict());
    }

    private User saveUser(String email) {
        return userRepository.saveAndFlush(User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode("Passw0rd!"))
                .role(UserRole.ROLE_USER)
                .isEmailVerified(true)
                .dailyLearningLimit(20)
                .build());
    }

    private Deck saveDeck(User owner) {
        return deckRepository.saveAndFlush(Deck.builder()
                .author(owner)
                .name("Deck")
                .description("desc")
                .isPublic(false)
                .tags("tag")
                .build());
    }

    private String bearerFor(User user) {
        return "Bearer " + jwtTokenService.createAccessToken(user.getId().toString(), Map.of("role", user.getRole().name()));
    }
}
