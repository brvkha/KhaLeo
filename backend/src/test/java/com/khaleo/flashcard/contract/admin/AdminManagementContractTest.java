package com.khaleo.flashcard.contract.admin;

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
import com.khaleo.flashcard.service.persistence.RelationalPersistenceService;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@SuppressWarnings("null")
class AdminManagementContractTest extends IntegrationPersistenceTestBase {

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
    private RelationalPersistenceService relationalPersistenceService;

    private User admin;
    private User learner;
    private Deck deck;
    private Card card;

    @BeforeEach
    void setUp() {
        admin = userRepository.save(User.builder()
                .email("admin-contract@example.com")
                .passwordHash("hash")
                .role(UserRole.ROLE_ADMIN)
                .isEmailVerified(true)
                .dailyLearningLimit(100)
                .build());

        learner = userRepository.save(User.builder()
                .email("learner-contract@example.com")
                .passwordHash("hash")
                .role(UserRole.ROLE_USER)
                .isEmailVerified(true)
                .dailyLearningLimit(100)
                .build());

        deck = relationalPersistenceService.createDeck(
                learner.getId(),
                new RelationalPersistenceService.CreateDeckRequest("Admin Deck", "desc", null, false, "t"));

        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        learner.getId().toString(), null, java.util.Collections.emptyList()));

        card = relationalPersistenceService.createCard(
                deck.getId(),
                new RelationalPersistenceService.CreateCardRequest("front", null, "back", null));

        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    @Test
    void shouldRejectNonAdminStatsAccess() throws Exception {
        String userToken = tokenFor(learner);

        mockMvc.perform(get("/api/v1/admin/stats")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnAdminStats() throws Exception {
        String adminToken = tokenFor(admin);

        String response = mockMvc.perform(get("/api/v1/admin/stats")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(response).contains("totalUsers", "totalDecks", "totalCards", "reviewsLast24Hours", "generatedAt");
    }

    @Test
    void shouldBanUserDeleteDeckAndUpdateCard() throws Exception {
        String adminToken = tokenFor(admin);

        mockMvc.perform(post("/api/v1/admin/users/{userId}/ban", learner.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        assertThat(userRepository.findById(learner.getId()).orElseThrow().getBannedAt()).isNotNull();

        String updatePayload = objectMapper.writeValueAsString(Map.of("frontText", "updated-front", "backText", "updated-back"));
        mockMvc.perform(put("/api/v1/admin/cards/{cardId}", card.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(updatePayload))
                .andExpect(status().isOk());

        assertThat(cardRepository.findById(card.getId()).orElseThrow().getFrontText()).isEqualTo("updated-front");

        mockMvc.perform(delete("/api/v1/admin/decks/{deckId}", deck.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        assertThat(deckRepository.findById(deck.getId())).isEmpty();
    }

    private String tokenFor(User user) {
        return jwtTokenService.createAccessToken(user.getId().toString(), Map.of("role", user.getRole().name()));
    }
}
