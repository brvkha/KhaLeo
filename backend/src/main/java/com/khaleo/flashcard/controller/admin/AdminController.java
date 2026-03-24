package com.khaleo.flashcard.controller.admin;

import com.khaleo.flashcard.controller.admin.dto.AdminCardUpdateRequest;
import com.khaleo.flashcard.controller.admin.dto.AdminDeckModerationItemResponse;
import com.khaleo.flashcard.controller.admin.dto.AdminModerationActionResponse;
import com.khaleo.flashcard.controller.admin.dto.AdminStatsResponse;
import com.khaleo.flashcard.controller.admin.dto.AdminUserModerationItemResponse;
import com.khaleo.flashcard.controller.card.dto.CardResponse;
import com.khaleo.flashcard.controller.common.PagedResponse;
import com.khaleo.flashcard.entity.Card;
import com.khaleo.flashcard.service.admin.AdminModerationService;
import com.khaleo.flashcard.service.admin.AdminStatsService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public PagedResponse<AdminUserModerationItemResponse> listUsers(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir) {
        return PagedResponse.from(adminModerationService.listUsers(q, page, size, sortBy, sortDir)
                .map(item -> new AdminUserModerationItemResponse(
                        item.id(),
                        item.email(),
                        item.role(),
                        item.verified(),
                        item.banned(),
                        item.createdAt())));
    }

    @GetMapping("/decks")
    @PreAuthorize("hasRole('ADMIN')")
    public PagedResponse<AdminDeckModerationItemResponse> listDecks(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir) {
        return PagedResponse.from(adminModerationService.listDecks(q, page, size, sortBy, sortDir)
                .map(item -> new AdminDeckModerationItemResponse(
                        item.id(),
                        item.name(),
                        item.ownerEmail(),
                        item.isPublic(),
                        item.banned(),
                        item.cardCount(),
                        item.createdAt())));
    }

    @PostMapping("/decks/{deckId}/ban")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void banDeck(@PathVariable("deckId") UUID deckId) {
        adminModerationService.banDeck(deckId);
    }

    @PostMapping("/users/{userId}/unban")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void unbanUser(@PathVariable("userId") UUID userId) {
        adminModerationService.unbanUser(userId);
    }

    @PostMapping("/decks/{deckId}/unban")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void unbanDeck(@PathVariable("deckId") UUID deckId) {
        adminModerationService.unbanDeck(deckId);
    }

    @GetMapping("/moderation-actions")
    @PreAuthorize("hasRole('ADMIN')")
    public PagedResponse<AdminModerationActionResponse> listModerationActions(
            @RequestParam(required = false) String adminUserId,
            @RequestParam(required = false) String adminEmail,
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir) {
        return PagedResponse.from(adminModerationService.listActions(adminUserId, adminEmail, targetType, status, page, size, sortBy, sortDir)
                .map(item -> new AdminModerationActionResponse(
                        item.id(),
                        item.adminUserId(),
                item.adminEmail(),
                        item.actionType(),
                        item.targetType(),
                        item.targetId(),
                item.targetDisplayName(),
                        item.status(),
                        item.reasonCode(),
                        item.createdAt())));
    }

        @GetMapping(value = "/moderation-actions/export", produces = "text/csv")
        @PreAuthorize("hasRole('ADMIN')")
        public ResponseEntity<String> exportModerationActionsCsv(
            @RequestParam(required = false) String adminUserId,
            @RequestParam(required = false) String adminEmail,
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer size) {
        String csv = adminModerationService.exportActionsCsv(adminUserId, adminEmail, targetType, status, size);
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("text/csv"))
            .header("Content-Disposition", "attachment; filename=moderation-actions.csv")
            .body(csv);
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
