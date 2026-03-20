package com.khaleo.flashcard.controller;

import com.khaleo.flashcard.service.persistence.PersistenceValidationException;
import com.khaleo.flashcard.service.study.StudyDomainException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(StudyDomainException.class)
    public ResponseEntity<Map<String, Object>> handleStudyDomain(StudyDomainException ex, HttpServletRequest request) {
        return ResponseEntity.status(ex.getStatus().value()).body(Map.of(
                "timestamp", Instant.now().toString(),
                "status", ex.getStatus().value(),
                "error", ex.getErrorCode().name(),
                "message", ex.getMessage(),
                "path", request.getRequestURI()));
    }

    @ExceptionHandler(PersistenceValidationException.class)
    public ResponseEntity<Map<String, Object>> handlePersistenceValidation(
            PersistenceValidationException ex,
            HttpServletRequest request) {
        HttpStatus status = switch (ex.getErrorCode()) {
            case AUTHORIZATION_DENIED, BANNED_USER_REQUEST_DENIED -> HttpStatus.FORBIDDEN;
            case DECK_NOT_FOUND, DECK_NOT_PUBLIC, CARD_NOT_FOUND, USER_NOT_FOUND, IMPORT_LINK_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case INVALID_PAGINATION, INVALID_SEARCH_CRITERIA, INVALID_CONFLICT_RESOLUTION_CHOICE -> HttpStatus.BAD_REQUEST;
            case DUPLICATE_EMAIL -> HttpStatus.CONFLICT;
            default -> HttpStatus.UNPROCESSABLE_ENTITY;
        };

        return ResponseEntity.status(status).body(Map.of(
                "timestamp", Instant.now().toString(),
                "status", status.value(),
                "error", ex.getErrorCode().name(),
                "message", ex.getMessage(),
                "path", request.getRequestURI()));
    }
}
