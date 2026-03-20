package com.khaleo.flashcard.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@SuppressWarnings("null")
class StudySessionFlowIntegrationTest extends IntegrationPersistenceTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DeckRepository deckRepository;

    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private RelationalPersistenceService relationalPersistenceService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Test
    void shouldReturnNextCardsAndAllowRating() throws Exception {
        User actor = saveUser("study-flow@example.com", true);
        Deck deck = saveDeck(actor, "Study Deck");
        Card card = saveCard(deck, "front", "back");

        String bearer = bearerFor(actor);

        mockMvc.perform(get("/api/v1/study-session/decks/{deckId}/next-cards", deck.getId())
                        .header("Authorization", bearer))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/study-session/cards/{cardId}/rate", card.getId())
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":\"GOOD\",\"timeSpentMs\":1200}"))
                .andExpect(status().isOk());
    }

    private User saveUser(String email, boolean verified) {
        return userRepository.saveAndFlush(User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode("Passw0rd!"))
                .role(UserRole.ROLE_USER)
                .isEmailVerified(verified)
                .build());
    }

    private Deck saveDeck(User author, String name) {
        return deckRepository.saveAndFlush(Deck.builder()
                .author(author)
                .name(name)
                .description("desc")
                .isPublic(false)
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

    private String bearerFor(User user) {
        return "Bearer " + jwtTokenService.createAccessToken(
                user.getId().toString(),
                Map.of("role", user.getRole().name()));
    }
}
