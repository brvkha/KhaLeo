package com.khaleo.flashcard.service.persistence;

import lombok.Getter;

@Getter
public class PersistenceValidationException extends RuntimeException {

    private final PersistenceErrorCode errorCode;

    public PersistenceValidationException(PersistenceErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public PersistenceValidationException(PersistenceErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public enum PersistenceErrorCode {
        DUPLICATE_EMAIL,
        INVALID_DAILY_LEARNING_LIMIT,
        INVALID_CARD_CONTENT,
        MISSING_RELATIONSHIP,
        DECK_NOT_FOUND,
        DECK_NOT_PUBLIC,
        CARD_NOT_FOUND,
        USER_NOT_FOUND,
        IMPORT_LINK_NOT_FOUND,
        INVALID_CONFLICT_RESOLUTION_CHOICE,
        AUTHORIZATION_DENIED,
        BANNED_USER_REQUEST_DENIED,
        INVALID_PAGINATION,
        INVALID_SEARCH_CRITERIA,
        MEDIA_TYPE_NOT_ALLOWED,
        MEDIA_SIZE_EXCEEDED,
        MEDIA_EXTENSION_MISMATCH,
        MEDIA_AUTH_RATE_LIMIT_EXCEEDED,
        MEDIA_REFERENCE_CONFLICT,
        OPTIMISTIC_LOCK_CONFLICT,
        VALIDATION_REJECTED
    }
}
