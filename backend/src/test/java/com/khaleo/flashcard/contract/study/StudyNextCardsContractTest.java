package com.khaleo.flashcard.contract.study;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.khaleo.flashcard.entity.Card;
import com.khaleo.flashcard.entity.CardLearningState;
import com.khaleo.flashcard.entity.Deck;
import com.khaleo.flashcard.entity.User;
import com.khaleo.flashcard.entity.enums.CardLearningStateType;
import com.khaleo.flashcard.entity.enums.UserRole;
import com.khaleo.flashcard.integration.support.IntegrationPersistenceTestBase;
import com.khaleo.flashcard.repository.CardLearningStateRepository;
import com.khaleo.flashcard.repository.CardRepository;
import com.khaleo.flashcard.repository.DeckRepository;
import com.khaleo.flashcard.repository.UserRepository;
import com.khaleo.flashcard.service.auth.JwtTokenService;
import java.time.Instant;
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
class StudyNextCardsContractTest extends IntegrationPersistenceTestBase {

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
    private CardLearningStateRepository cardLearningStateRepository;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void shouldReturnCardsInLearningReviewNewOrder() throws Exception {
        User owner = createUser("study-contract-owner@example.com", 5);
        Deck deck = createDeck(owner, false);

        Card learningCard = createCard(deck, "learning-front", "learning-back");
        Card reviewCard = createCard(deck, "review-front", "review-back");
        Card newCard = createCard(deck, "new-front", "new-back");

        saveState(owner, learningCard, CardLearningStateType.LEARNING, Instant.now().minusSeconds(60));
        saveState(owner, reviewCard, CardLearningStateType.REVIEW, Instant.now().minusSeconds(30));

        String body = mockMvc.perform(get("/api/v1/study/decks/{deckId}/next-cards", deck.getId())
                        .header("Authorization", bearerFor(owner))
                        .queryParam("size", "10"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(body);
        assertThat(json.get("items").size()).isEqualTo(3);
        assertThat(json.get("items").get(0).get("cardId").asText()).isEqualTo(learningCard.getId().toString());
        assertThat(json.get("items").get(1).get("cardId").asText()).isEqualTo(reviewCard.getId().toString());
        assertThat(json.get("items").get(2).get("cardId").asText()).isEqualTo(newCard.getId().toString());
    }

    @Test
    void shouldRejectInvalidPageSize() throws Exception {
        User owner = createUser("study-contract-owner-2@example.com", 5);
        Deck deck = createDeck(owner, true);

        mockMvc.perform(get("/api/v1/study/decks/{deckId}/next-cards", deck.getId())
                        .header("Authorization", bearerFor(owner))
                        .queryParam("size", "0"))
                .andExpect(status().isBadRequest());
    }

    private User createUser(String email, int dailyLimit) {
        User user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode("Passw0rd!"))
                .role(UserRole.ROLE_USER)
                .isEmailVerified(true)
                .dailyLearningLimit(dailyLimit)
                .build();
        return userRepository.saveAndFlush(user);
    }

    private Deck createDeck(User owner, boolean isPublic) {
        return deckRepository.saveAndFlush(Deck.builder()
                .author(owner)
                .name("Deck")
                .description("desc")
                .isPublic(isPublic)
                .tags("tag")
                .build());
    }

    private Card createCard(Deck deck, String front, String back) {
        return cardRepository.saveAndFlush(Card.builder()
                .deck(deck)
                .frontText(front)
                .backText(back)
                .build());
    }

    private void saveState(User user, Card card, CardLearningStateType type, Instant dueAt) {
        cardLearningStateRepository.saveAndFlush(CardLearningState.builder()
                .user(user)
                .card(card)
                .state(type)
                .intervalInDays(1)
                .nextReviewDate(dueAt)
                .easeFactor(java.math.BigDecimal.valueOf(2.5))
                .learningStepGoodCount(1)
                .build());
    }

    private String bearerFor(User user) {
        return "Bearer " + jwtTokenService.createAccessToken(user.getId().toString(), Map.of("role", user.getRole().name()));
    }
}
