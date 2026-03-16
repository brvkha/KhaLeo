package com.khaleo.flashcard.controller.auth.dto;

import java.util.UUID;

public record RegisterResponse(UUID userId, String email, boolean verificationRequired) {
}
