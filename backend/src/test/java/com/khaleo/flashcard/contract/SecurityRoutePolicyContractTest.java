package com.khaleo.flashcard.contract;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.khaleo.flashcard.integration.support.IntegrationPersistenceTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@SuppressWarnings("null")
class SecurityRoutePolicyContractTest extends IntegrationPersistenceTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldAllowGuestOnlyForPublicDeckDiscoveryAndAuthEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/public/decks"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRequireAuthenticationForAllProtectedApiRoutes() throws Exception {
        String deckId = "11111111-1111-1111-1111-111111111111";

        mockMvc.perform(get("/api/v1/decks"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/decks/{id}", deckId))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/private/decks"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/decks/{deckId}/cards/search", deckId))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/decks/{deckId}/cards", deckId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"frontText\":\"F\",\"backText\":\"B\"}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/study/decks/{deckId}/next-cards", deckId))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/admin/stats"))
                .andExpect(status().isUnauthorized());
    }
}
