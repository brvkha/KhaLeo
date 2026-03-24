package com.khaleo.flashcard.service.admin;

import com.khaleo.flashcard.config.observability.NewRelicAuthInstrumentation;
import com.khaleo.flashcard.config.PaginationConfig;
import com.khaleo.flashcard.controller.admin.dto.AdminCardUpdateRequest;
import com.khaleo.flashcard.entity.AdminModerationAction;
import com.khaleo.flashcard.entity.Card;
import com.khaleo.flashcard.entity.Deck;
import com.khaleo.flashcard.entity.User;
import com.khaleo.flashcard.entity.enums.AdminActionStatus;
import com.khaleo.flashcard.entity.enums.AdminActionType;
import com.khaleo.flashcard.entity.enums.AdminTargetType;
import com.khaleo.flashcard.repository.AdminModerationActionRepository;
import com.khaleo.flashcard.repository.CardRepository;
import com.khaleo.flashcard.repository.DeckRepository;
import com.khaleo.flashcard.repository.UserRepository;
import com.khaleo.flashcard.service.auth.AuthDomainException;
import com.khaleo.flashcard.service.auth.AuthErrorCode;
import com.khaleo.flashcard.service.persistence.DeckCardAccessGuard;
import com.khaleo.flashcard.service.persistence.PersistenceValidationException;
import com.khaleo.flashcard.service.persistence.PersistenceValidationExceptionMapper;
import com.khaleo.flashcard.service.persistence.RelationalPersistenceService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminModerationService {

    private final UserRepository userRepository;
    private final DeckRepository deckRepository;
    private final CardRepository cardRepository;
    private final RelationalPersistenceService relationalPersistenceService;
    private final AdminModerationActionRepository adminModerationActionRepository;
    private final DeckCardAccessGuard deckCardAccessGuard;
    private final PersistenceValidationExceptionMapper exceptionMapper;
    private final NewRelicAuthInstrumentation newRelicAuthInstrumentation;
    private final PaginationConfig paginationConfig;

    @Transactional(readOnly = true)
    public Page<AdminUserModerationItem> listUsers(String query, Integer page, Integer size, String sortBy, String sortDir) {
        String normalizedQuery = query == null || query.isBlank() ? null : query.trim();
        Pageable pageable = resolveAdminPageable(page, size, sortBy, sortDir, List.of("createdAt", "email", "role", "bannedAt"));
        return userRepository.searchForAdmin(normalizedQuery, pageable)
                .map(user -> new AdminUserModerationItem(
                        user.getId(),
                        user.getEmail(),
                        user.getRole().name(),
                        Boolean.TRUE.equals(user.getIsEmailVerified()),
                        user.getBannedAt() != null,
                        user.getCreatedAt()));
    }

    @Transactional(readOnly = true)
    public Page<AdminDeckModerationItem> listDecks(String query, Integer page, Integer size, String sortBy, String sortDir) {
        String normalizedQuery = query == null || query.isBlank() ? null : query.trim();
        Pageable pageable = resolveAdminPageable(page, size, sortBy, sortDir, List.of("createdAt", "name", "isPublic", "bannedAt"));
        return deckRepository.searchForAdmin(normalizedQuery, pageable)
                .map(deck -> new AdminDeckModerationItem(
                        deck.getId(),
                        deck.getName(),
                        deck.getAuthor().getEmail(),
                        Boolean.TRUE.equals(deck.getIsPublic()),
                        deck.getBannedAt() != null,
                        cardRepository.countByDeckId(deck.getId()),
                        deck.getCreatedAt()));
    }

                @Transactional(readOnly = true)
                public Page<AdminModerationActionItem> listActions(
                        String adminUserId,
                        String adminEmail,
                    String targetType,
                    String status,
                    Integer page,
                    Integer size,
                    String sortBy,
                    String sortDir) {
                    UUID adminUserIdFilter = parseUuidFilter(adminUserId, "adminUserId");
                    String adminEmailFilter = normalizeTextFilter(adminEmail);
                AdminTargetType targetTypeFilter = parseTargetType(targetType);
                AdminActionStatus statusFilter = parseStatus(status);
                Pageable pageable = resolveAdminPageable(page, size, sortBy, sortDir, List.of("createdAt", "actionType", "targetType", "status"));

                    return adminModerationActionRepository.searchForAdmin(adminUserIdFilter, adminEmailFilter, targetTypeFilter, statusFilter, pageable)
                        .map(action -> {
                            String resolvedAdminEmail = userRepository.findById(action.getAdminUserId())
                                .map(User::getEmail)
                                .orElse("unknown");
                            String targetDisplayName = resolveTargetDisplayName(action.getTargetType(), action.getTargetId());

                            return new AdminModerationActionItem(
                                action.getId(),
                                action.getAdminUserId(),
                                resolvedAdminEmail,
                                action.getActionType().name(),
                                action.getTargetType().name(),
                                action.getTargetId(),
                                targetDisplayName,
                                action.getStatus().name(),
                                action.getReasonCode(),
                                action.getCreatedAt());
                        });
                }

                    @Transactional(readOnly = true)
                    public String exportActionsCsv(
                        String adminUserId,
                        String adminEmail,
                        String targetType,
                        String status,
                        Integer size) {
                    Page<AdminModerationActionItem> page = listActions(
                        adminUserId,
                        adminEmail,
                        targetType,
                        status,
                        0,
                        size == null ? 200 : Math.min(size, 1000),
                        "createdAt",
                        "desc");

                    String header = "id,createdAt,adminUserId,adminEmail,actionType,targetType,targetId,targetDisplayName,status,reasonCode";
                    List<String> rows = page.getContent().stream()
                        .map(item -> String.join(",",
                            escapeCsv(item.id().toString()),
                            escapeCsv(item.createdAt().toString()),
                            escapeCsv(item.adminUserId().toString()),
                            escapeCsv(item.adminEmail()),
                            escapeCsv(item.actionType()),
                            escapeCsv(item.targetType()),
                            escapeCsv(item.targetId().toString()),
                            escapeCsv(item.targetDisplayName()),
                            escapeCsv(item.status()),
                            escapeCsv(item.reasonCode())))
                        .collect(Collectors.toList());

                    return header + "\n" + String.join("\n", rows);
                    }

    public void banUser(UUID targetUserId) {
        UUID actorId = deckCardAccessGuard.requireAuthenticatedUserId("ban", "user", targetUserId.toString());
        if (actorId.equals(targetUserId)) {
            throw new AuthDomainException(HttpStatus.BAD_REQUEST, AuthErrorCode.INVALID_REQUEST, "Admins cannot ban themselves.");
        }

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> exceptionMapper.userNotFound(targetUserId));

        targetUser.setBannedAt(Instant.now());
        targetUser.setBannedBy(actorId);
        userRepository.save(targetUser);

        writeAudit(actorId, AdminActionType.USER_BAN, AdminTargetType.USER, targetUserId, AdminActionStatus.SUCCESS, null);
        newRelicAuthInstrumentation.recordAuthOutcome("admin_user_ban_success", Map.of("adminUserId", actorId, "targetUserId", targetUserId));
        newRelicAuthInstrumentation.recordAdminModerationOutcome("USER_BAN", "success", Map.of("adminUserId", actorId, "targetUserId", targetUserId));
    }

    public void unbanUser(UUID targetUserId) {
        UUID actorId = deckCardAccessGuard.requireAuthenticatedUserId("unban", "user", targetUserId.toString());
        if (actorId.equals(targetUserId)) {
            throw new AuthDomainException(HttpStatus.BAD_REQUEST, AuthErrorCode.INVALID_REQUEST, "Admins cannot unban themselves.");
        }

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> exceptionMapper.userNotFound(targetUserId));

        targetUser.setBannedAt(null);
        targetUser.setBannedBy(null);
        userRepository.save(targetUser);

        writeAudit(actorId, AdminActionType.USER_BAN, AdminTargetType.USER, targetUserId, AdminActionStatus.SUCCESS, "UNBAN");
        newRelicAuthInstrumentation.recordAdminModerationOutcome("USER_UNBAN", "success", Map.of("adminUserId", actorId, "targetUserId", targetUserId));
    }

    public void banDeck(UUID targetDeckId) {
        UUID actorId = deckCardAccessGuard.requireAuthenticatedUserId("ban", "deck", targetDeckId.toString());
        Deck targetDeck = deckRepository.findById(targetDeckId)
                .orElseThrow(() -> exceptionMapper.deckNotFound(targetDeckId));

        targetDeck.setBannedAt(Instant.now());
        targetDeck.setBannedBy(actorId);
        targetDeck.setIsPublic(Boolean.FALSE);
        deckRepository.save(targetDeck);

        writeAudit(actorId, AdminActionType.DECK_BAN, AdminTargetType.DECK, targetDeckId, AdminActionStatus.SUCCESS, null);
        newRelicAuthInstrumentation.recordAdminModerationOutcome("DECK_BAN", "success", Map.of("adminUserId", actorId, "deckId", targetDeckId));
    }

    public void unbanDeck(UUID targetDeckId) {
        UUID actorId = deckCardAccessGuard.requireAuthenticatedUserId("unban", "deck", targetDeckId.toString());
        Deck targetDeck = deckRepository.findById(targetDeckId)
                .orElseThrow(() -> exceptionMapper.deckNotFound(targetDeckId));

        targetDeck.setBannedAt(null);
        targetDeck.setBannedBy(null);
        deckRepository.save(targetDeck);

        writeAudit(actorId, AdminActionType.DECK_BAN, AdminTargetType.DECK, targetDeckId, AdminActionStatus.SUCCESS, "UNBAN");
        newRelicAuthInstrumentation.recordAdminModerationOutcome("DECK_UNBAN", "success", Map.of("adminUserId", actorId, "deckId", targetDeckId));
    }

    public void deleteDeck(UUID deckId) {
        UUID actorId = deckCardAccessGuard.requireAuthenticatedUserId("delete", "deck", deckId.toString());
        try {
            relationalPersistenceService.deleteDeck(deckId);
            writeAudit(actorId, AdminActionType.DECK_DELETE, AdminTargetType.DECK, deckId, AdminActionStatus.SUCCESS, null);
            newRelicAuthInstrumentation.recordAdminModerationOutcome("DECK_DELETE", "success", Map.of("adminUserId", actorId, "deckId", deckId));
        } catch (RuntimeException ex) {
            writeAudit(actorId, AdminActionType.DECK_DELETE, AdminTargetType.DECK, deckId, AdminActionStatus.FAILURE, ex.getClass().getSimpleName());
            newRelicAuthInstrumentation.recordAdminModerationFailure("DECK_DELETE", ex.getClass().getSimpleName(), Map.of("adminUserId", actorId, "deckId", deckId));
            throw ex;
        }
    }

    public Card updateCard(UUID cardId, AdminCardUpdateRequest request) {
        UUID actorId = deckCardAccessGuard.requireAuthenticatedUserId("update", "card", cardId.toString());
        try {
            Card updated = relationalPersistenceService.updateCard(
                    cardId,
                    new RelationalPersistenceService.UpdateCardRequest(
                            request.frontText(),
                            request.frontMediaUrl(),
                            request.backText(),
                            request.backMediaUrl()));
            writeAudit(actorId, AdminActionType.CARD_EDIT, AdminTargetType.CARD, cardId, AdminActionStatus.SUCCESS, null);
            newRelicAuthInstrumentation.recordAdminModerationOutcome("CARD_EDIT", "success", Map.of("adminUserId", actorId, "cardId", cardId));
            return updated;
        } catch (PersistenceValidationException ex) {
            writeAudit(actorId, AdminActionType.CARD_EDIT, AdminTargetType.CARD, cardId, AdminActionStatus.FAILURE, ex.getErrorCode().name());
            newRelicAuthInstrumentation.recordAdminModerationFailure("CARD_EDIT", ex.getErrorCode().name(), Map.of("adminUserId", actorId, "cardId", cardId));
            throw ex;
        }
    }

    private void writeAudit(
            UUID adminUserId,
            AdminActionType actionType,
            AdminTargetType targetType,
            UUID targetId,
            AdminActionStatus status,
            String reasonCode) {
        adminModerationActionRepository.save(AdminModerationAction.builder()
                .adminUserId(adminUserId)
                .actionType(actionType)
                .targetType(targetType)
                .targetId(targetId)
                .status(status)
                .reasonCode(reasonCode)
                .build());
    }

    private Pageable resolveAdminPageable(
            Integer page,
            Integer size,
            String sortBy,
            String sortDir,
            List<String> allowedSortFields) {
        Pageable base = paginationConfig.resolvePageable(page, size);
        String resolvedSortBy = (sortBy == null || sortBy.isBlank()) ? "createdAt" : sortBy.trim();

        if (!allowedSortFields.contains(resolvedSortBy)) {
            throw new AuthDomainException(HttpStatus.BAD_REQUEST, AuthErrorCode.INVALID_REQUEST, "Unsupported sort field: " + resolvedSortBy);
        }

        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return PageRequest.of(base.getPageNumber(), base.getPageSize(), Sort.by(direction, resolvedSortBy));
    }

    private AdminTargetType parseTargetType(String targetType) {
        if (targetType == null || targetType.isBlank()) {
            return null;
        }

        try {
            return AdminTargetType.valueOf(targetType.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new AuthDomainException(HttpStatus.BAD_REQUEST, AuthErrorCode.INVALID_REQUEST, "Invalid targetType filter.");
        }
    }

    private AdminActionStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }

        try {
            return AdminActionStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new AuthDomainException(HttpStatus.BAD_REQUEST, AuthErrorCode.INVALID_REQUEST, "Invalid status filter.");
        }
    }

    private UUID parseUuidFilter(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return UUID.fromString(value.trim());
        } catch (IllegalArgumentException ex) {
            throw new AuthDomainException(HttpStatus.BAD_REQUEST, AuthErrorCode.INVALID_REQUEST, "Invalid " + fieldName + " filter.");
        }
    }

    private String normalizeTextFilter(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    private String resolveTargetDisplayName(com.khaleo.flashcard.entity.enums.AdminTargetType targetType, UUID targetId) {
        return switch (targetType) {
            case USER -> userRepository.findById(targetId).map(User::getEmail).orElse("unknown user");
            case DECK -> deckRepository.findById(targetId).map(Deck::getName).orElse("unknown deck");
            case CARD -> cardRepository.findById(targetId)
                    .map(card -> {
                        String deckName = card.getDeck() != null ? card.getDeck().getName() : "unknown deck";
                        String front = card.getFrontText() == null ? "" : card.getFrontText();
                        return deckName + " :: " + front;
                    })
                    .orElse("unknown card");
        };
    }

    private String escapeCsv(String value) {
        String safe = value == null ? "" : value;
        String escaped = safe.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }

    public record AdminUserModerationItem(
            UUID id,
            String email,
            String role,
            boolean verified,
            boolean banned,
            Instant createdAt) {
    }

    public record AdminDeckModerationItem(
            UUID id,
            String name,
            String ownerEmail,
            boolean isPublic,
            boolean banned,
            long cardCount,
            Instant createdAt) {
    }

    public record AdminModerationActionItem(
            UUID id,
            UUID adminUserId,
            String adminEmail,
            String actionType,
            String targetType,
            UUID targetId,
            String targetDisplayName,
            String status,
            String reasonCode,
            Instant createdAt) {
    }
}
