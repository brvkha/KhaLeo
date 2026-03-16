package com.khaleo.flashcard.contract.study;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
class StudyRateCardContractTest extends IntegrationPersistenceTestBase {

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
    void shouldRateCardAndReturnSchedulingPayload() throws Exception {
        User owner = saveUser("rate-contract-owner@example.com");
        Deck deck = saveDeck(owner);
        Card card = saveCard(deck);

        String body = objectMapper.writeValueAsString(Map.of("rating", "GOOD", "timeSpentMs", 1200));

        String response = mockMvc.perform(post("/api/v1/study/cards/{cardId}/rate", card.getId())
                        .header("Authorization", bearerFor(owner))
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(response).contains("cardId");
        assertThat(response).contains("state");
        assertThat(response).contains("newInterval");
        assertThat(response).contains("newEaseFactor");
    }

    @Test
    void shouldRejectInvalidRatePayload() throws Exception {
        User owner = saveUser("rate-contract-owner-2@example.com");
        Deck deck = saveDeck(owner);
        Card card = saveCard(deck);

        String body = objectMapper.writeValueAsString(Map.of("rating", "GOOD", "timeSpentMs", -1));

        mockMvc.perform(post("/api/v1/study/cards/{cardId}/rate", card.getId())
                        .header("Authorization", bearerFor(owner))
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isBadRequest());
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

    private Card saveCard(Deck deck) {
        return cardRepository.saveAndFlush(Card.builder()
                .deck(deck)
                .frontText("front")
                .backText("back")
                .build());
    }

    private String bearerFor(User user) {
        return "Bearer " + jwtTokenService.createAccessToken(user.getId().toString(), Map.of("role", user.getRole().name()));
    }
}
