package com.khaleo.flashcard.controller.admin;

import com.khaleo.flashcard.controller.admin.dto.AdminCardUpdateRequest;
import com.khaleo.flashcard.controller.admin.dto.AdminStatsResponse;
import com.khaleo.flashcard.controller.card.dto.CardResponse;
import com.khaleo.flashcard.entity.Card;
import com.khaleo.flashcard.service.admin.AdminModerationService;
import com.khaleo.flashcard.service.admin.AdminStatsService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminStatsService adminStatsService;
    private final AdminModerationService adminModerationService;

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public AdminStatsResponse stats() {
        AdminStatsService.StatsSnapshot snapshot = adminStatsService.getPlatformStats();
        return new AdminStatsResponse(
                snapshot.totalUsers(),
                snapshot.totalDecks(),
                snapshot.totalCards(),
                snapshot.reviewsLast24Hours(),
                snapshot.generatedAt());
    }

    @PostMapping("/users/{userId}/ban")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void banUser(@PathVariable("userId") UUID userId) {
        adminModerationService.banUser(userId);
    }

    @DeleteMapping("/decks/{deckId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteDeck(@PathVariable("deckId") UUID deckId) {
        adminModerationService.deleteDeck(deckId);
    }

    @PutMapping("/cards/{cardId}")
    @PreAuthorize("hasRole('ADMIN')")
    public CardResponse updateCard(@PathVariable("cardId") UUID cardId, @Valid @RequestBody AdminCardUpdateRequest request) {
        Card updated = adminModerationService.updateCard(cardId, request);
        return CardResponse.from(updated);
    }
}
