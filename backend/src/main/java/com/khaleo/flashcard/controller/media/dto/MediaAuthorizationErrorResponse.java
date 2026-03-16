package com.khaleo.flashcard.controller.media.dto;

public record MediaAuthorizationErrorResponse(
        String code,
        String message) {
}
