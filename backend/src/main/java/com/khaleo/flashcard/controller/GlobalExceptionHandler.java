package com.khaleo.flashcard.controller;

import com.khaleo.flashcard.service.study.StudyDomainException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.Map;
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
}
