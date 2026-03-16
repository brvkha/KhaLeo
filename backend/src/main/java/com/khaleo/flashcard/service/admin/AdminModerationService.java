package com.khaleo.flashcard.service.admin;

import com.khaleo.flashcard.config.observability.NewRelicAuthInstrumentation;
import com.khaleo.flashcard.controller.admin.dto.AdminCardUpdateRequest;
import com.khaleo.flashcard.entity.AdminModerationAction;
import com.khaleo.flashcard.entity.Card;
import com.khaleo.flashcard.entity.User;
import com.khaleo.flashcard.entity.enums.AdminActionStatus;
import com.khaleo.flashcard.entity.enums.AdminActionType;
import com.khaleo.flashcard.entity.enums.AdminTargetType;
import com.khaleo.flashcard.repository.AdminModerationActionRepository;
import com.khaleo.flashcard.repository.UserRepository;
import com.khaleo.flashcard.service.persistence.DeckCardAccessGuard;
import com.khaleo.flashcard.service.persistence.PersistenceValidationException;
import com.khaleo.flashcard.service.persistence.PersistenceValidationExceptionMapper;
import com.khaleo.flashcard.service.persistence.RelationalPersistenceService;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminModerationService {

    private final UserRepository userRepository;
    private final RelationalPersistenceService relationalPersistenceService;
    private final AdminModerationActionRepository adminModerationActionRepository;
    private final DeckCardAccessGuard deckCardAccessGuard;
    private final PersistenceValidationExceptionMapper exceptionMapper;
    private final NewRelicAuthInstrumentation newRelicAuthInstrumentation;

    public void banUser(UUID targetUserId) {
        UUID actorId = deckCardAccessGuard.requireAuthenticatedUserId("ban", "user", targetUserId.toString());
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> exceptionMapper.userNotFound(targetUserId));

        targetUser.setBannedAt(Instant.now());
        targetUser.setBannedBy(actorId);
        userRepository.save(targetUser);

        writeAudit(actorId, AdminActionType.USER_BAN, AdminTargetType.USER, targetUserId, AdminActionStatus.SUCCESS, null);
        newRelicAuthInstrumentation.recordAuthOutcome("admin_user_ban_success", Map.of("adminUserId", actorId, "targetUserId", targetUserId));
        newRelicAuthInstrumentation.recordAdminModerationOutcome("USER_BAN", "success", Map.of("adminUserId", actorId, "targetUserId", targetUserId));
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
}
