package com.khaleo.flashcard.config.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.khaleo.flashcard.service.auth.AuthDomainException;
import com.khaleo.flashcard.service.auth.AuthErrorCode;
import com.khaleo.flashcard.service.persistence.PersistenceValidationException;
import com.khaleo.flashcard.service.persistence.PersistenceValidationException.PersistenceErrorCode;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@RequiredArgsConstructor
public class AuthExceptionHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @ExceptionHandler(AuthDomainException.class)
    public ResponseEntity<Map<String, Object>> handleAuthDomain(AuthDomainException ex, HttpServletRequest request) {
        HttpStatus status = ex.getStatus() == null ? HttpStatus.BAD_REQUEST : ex.getStatus();
        return ResponseEntity.status(status.value()).body(errorBody(
            status,
                ex.getErrorCode().name(),
                ex.getMessage(),
                request.getRequestURI()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        return ResponseEntity.badRequest().body(errorBody(
                HttpStatus.BAD_REQUEST,
                AuthErrorCode.INVALID_REQUEST.name(),
                ex.getMessage(),
                request.getRequestURI()));
    }

    @ExceptionHandler(PersistenceValidationException.class)
    public ResponseEntity<Map<String, Object>> handlePersistenceValidation(
            PersistenceValidationException ex,
            HttpServletRequest request) {
        HttpStatus status = mapPersistenceStatus(ex.getErrorCode());
        return ResponseEntity.status(status.value()).body(errorBody(
                status,
                ex.getErrorCode().name(),
                ex.getMessage(),
                request.getRequestURI()));
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException) throws IOException, ServletException {
        writeError(response, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Authentication is required.", request.getRequestURI());
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException, ServletException {
        writeError(response, HttpStatus.FORBIDDEN, "FORBIDDEN", "Access is denied.", request.getRequestURI());
    }

    private void writeError(HttpServletResponse response, HttpStatus status, String error, String message, String path)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/json");
        objectMapper.writeValue(response.getWriter(), errorBody(status, error, message, path));
    }

    private Map<String, Object> errorBody(HttpStatus status, String error, String message, String path) {
        return Map.of(
                "timestamp", Instant.now().toString(),
                "status", status.value(),
                "error", error,
                "message", message,
                "path", path);
    }

    private HttpStatus mapPersistenceStatus(PersistenceErrorCode errorCode) {
        if (errorCode == null) {
            return HttpStatus.BAD_REQUEST;
        }

        return switch (errorCode) {
            case AUTHORIZATION_DENIED, BANNED_USER_REQUEST_DENIED -> HttpStatus.FORBIDDEN;
            case USER_NOT_FOUND, DECK_NOT_FOUND, CARD_NOT_FOUND, MISSING_RELATIONSHIP -> HttpStatus.NOT_FOUND;
            case MEDIA_AUTH_RATE_LIMIT_EXCEEDED -> HttpStatus.TOO_MANY_REQUESTS;
            case DUPLICATE_EMAIL -> HttpStatus.CONFLICT;
            default -> HttpStatus.BAD_REQUEST;
        };
    }
}
