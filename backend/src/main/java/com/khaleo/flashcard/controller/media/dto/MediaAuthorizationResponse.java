package com.khaleo.flashcard.controller.media.dto;

public record MediaAuthorizationResponse(
        String objectKey,
        String uploadUrl,
        long expiresInSeconds) {
}
