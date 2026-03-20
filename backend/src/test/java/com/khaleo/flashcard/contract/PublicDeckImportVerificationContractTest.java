package com.khaleo.flashcard.contract;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
class PublicDeckImportVerificationContractTest extends IntegrationPersistenceTestBase {

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
    void shouldDenyImportForAuthenticatedButUnverifiedUser() throws Exception {
        User owner = saveUser("public-owner-verified@example.com", true);
        User actor = saveUser("public-actor-unverified@example.com", false);
        Deck deck = saveDeck(owner, "Public For Verification Gate", true);

        mockMvc.perform(post("/api/v1/public/decks/{deckId}/import", deck.getId())
                        .header("Authorization", bearerFor(actor)))
                .andExpect(status().isForbidden());
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

    private String bearerFor(User user) {
        return "Bearer " + jwtTokenService.createAccessToken(
                user.getId().toString(),
                Map.of("role", user.getRole().name()));
    }
}
