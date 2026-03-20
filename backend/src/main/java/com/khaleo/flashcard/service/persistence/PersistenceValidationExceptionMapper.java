package com.khaleo.flashcard.service.persistence;

import com.khaleo.flashcard.service.persistence.PersistenceValidationException.PersistenceErrorCode;
import java.util.Locale;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PersistenceValidationExceptionMapper {

    public RuntimeException mapCreateUserFailure(RuntimeException ex, String email) {
        String detail = extractDetail(ex);
        String lowerDetail = detail.toLowerCase(Locale.ROOT);

        if (containsAny(lowerDetail, "uk_users_email", "duplicate", "unique", "email")) {
            return logged(
                    PersistenceErrorCode.DUPLICATE_EMAIL,
                    "Duplicate email address rejected: " + email,
                    ex,
                    "persistence_user_rejected_duplicate_email");
        }

        if (containsAny(lowerDetail, "daily_learning_limit", "between 1 and 9999", "ck_users_daily_learning_limit")) {
            return logged(
                    PersistenceErrorCode.INVALID_DAILY_LEARNING_LIMIT,
                    "dailyLearningLimit must be between 1 and 9999.",
                    ex,
                    "persistence_user_rejected_invalid_daily_limit");
        }

        return logged(
                PersistenceErrorCode.VALIDATION_REJECTED,
                "User persistence rejected by validation rules.",
                ex,
                "persistence_user_rejected_validation");
    }

    public RuntimeException mapCreateCardFailure(RuntimeException ex, UUID deckId) {
        String detail = extractDetail(ex).toLowerCase(Locale.ROOT);
        if (containsAny(detail, "front and back must each contain", "ck_cards_front_content", "ck_cards_back_content")) {
            return logged(
                    PersistenceErrorCode.INVALID_CARD_CONTENT,
                    "Card front and back must each contain text or media content.",
                    ex,
                    "persistence_card_rejected_content");
        }

        return logged(
                PersistenceErrorCode.VALIDATION_REJECTED,
                "Card persistence rejected by validation rules for deckId=" + deckId,
                ex,
                "persistence_card_rejected_validation");
    }

    public RuntimeException mapCreateDeckFailure(RuntimeException ex, UUID authorId) {
        return logged(
                PersistenceErrorCode.VALIDATION_REJECTED,
                "Deck persistence rejected for authorId=" + authorId,
                ex,
                "persistence_deck_rejected_validation");
    }

    public RuntimeException mapLearningStateFailure(RuntimeException ex, UUID userId, UUID cardId) {
        if (ex instanceof ObjectOptimisticLockingFailureException) {
            return mapOptimisticLockFailure(ex, userId, cardId);
        }

        String detail = extractDetail(ex).toLowerCase(Locale.ROOT);
        if (containsAny(detail, "uk_learning_state_user_card", "duplicate", "unique")) {
            return logged(
                    PersistenceErrorCode.VALIDATION_REJECTED,
                    "Active learning state already exists for userId=" + userId + " and cardId=" + cardId,
                    ex,
                    "persistence_learning_state_rejected_unique");
        }

        return logged(
                PersistenceErrorCode.VALIDATION_REJECTED,
                "Learning-state persistence rejected for userId=" + userId + " and cardId=" + cardId,
                ex,
                "persistence_learning_state_rejected_validation");
    }

    public RuntimeException mapOptimisticLockFailure(Throwable ex, UUID userId, UUID cardId) {
        return logged(
                PersistenceErrorCode.OPTIMISTIC_LOCK_CONFLICT,
                "Optimistic lock conflict for userId=" + userId + " and cardId=" + cardId,
                ex,
                "persistence_learning_state_rejected_optimistic_lock");
    }

    public RuntimeException missingRelationship(String relation, String key) {
        return logged(
                PersistenceErrorCode.MISSING_RELATIONSHIP,
                "Missing required relationship: " + relation + " (" + key + ")",
                null,
                "persistence_rejected_missing_relationship");
    }

    public RuntimeException deckNotFound(UUID deckId) {
        return logged(
                PersistenceErrorCode.DECK_NOT_FOUND,
                "Deck not found: " + deckId,
                null,
                "persistence_rejected_deck_not_found");
    }

    public RuntimeException deckNotPublic(UUID deckId) {
        return logged(
                PersistenceErrorCode.DECK_NOT_PUBLIC,
                "Deck is not public: " + deckId,
                null,
                "persistence_rejected_deck_not_public");
    }

    public RuntimeException importLinkNotFound(UUID importLinkId) {
        return logged(
                PersistenceErrorCode.IMPORT_LINK_NOT_FOUND,
                "Import link not found: " + importLinkId,
                null,
                "persistence_rejected_import_link_not_found");
    }

    public RuntimeException invalidConflictResolutionChoice(String choice) {
        return logged(
                PersistenceErrorCode.INVALID_CONFLICT_RESOLUTION_CHOICE,
                "Invalid conflict resolution choice: " + choice,
                null,
                "persistence_rejected_invalid_conflict_resolution_choice");
    }

    public RuntimeException cardNotFound(UUID cardId) {
        return logged(
                PersistenceErrorCode.CARD_NOT_FOUND,
                "Card not found: " + cardId,
                null,
                "persistence_rejected_card_not_found");
    }

    public RuntimeException userNotFound(UUID userId) {
        return logged(
                PersistenceErrorCode.USER_NOT_FOUND,
                "User not found: " + userId,
                null,
                "persistence_rejected_user_not_found");
    }

    public RuntimeException authorizationDenied(String operation, String resourceType, String resourceKey) {
        return logged(
                PersistenceErrorCode.AUTHORIZATION_DENIED,
                "Authorization denied for operation=" + operation + " resource=" + resourceType + " key=" + resourceKey,
                null,
                "persistence_rejected_authorization_denied");
    }

    public RuntimeException bannedUserDenied(UUID userId, String operation, String resourceType, String resourceKey) {
        return logged(
                PersistenceErrorCode.BANNED_USER_REQUEST_DENIED,
                "Banned user denied for userId=" + userId + " operation=" + operation + " resource=" + resourceType + " key=" + resourceKey,
                null,
                "persistence_rejected_banned_user_denied");
    }

    public RuntimeException invalidPagination(Integer page, Integer size) {
        return logged(
                PersistenceErrorCode.INVALID_PAGINATION,
                "Invalid pagination request page=" + page + " size=" + size,
                null,
                "persistence_rejected_invalid_pagination");
    }

    private RuntimeException logged(
            PersistenceErrorCode code,
            String message,
            Throwable cause,
            String event) {

        if (cause == null) {
            log.warn("event={} code={} message={}", event, code, message);
            return new PersistenceValidationException(code, message);
        }

        log.warn("event={} code={} message={} reason={}", event, code, message, cause.getMessage());
        return new PersistenceValidationException(code, message, cause);
    }

    private String extractDetail(Throwable throwable) {
        Throwable root = throwable;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return root.getMessage() == null ? throwable.getMessage() : root.getMessage();
    }

    private boolean containsAny(String source, String... values) {
        if (source == null) {
            return false;
        }
        for (String value : values) {
            if (source.contains(value)) {
                return true;
            }
        }
        return false;
    }
}
