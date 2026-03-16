package com.khaleo.flashcard.controller.auth.dto;

public record LoginResponse(String accessToken, String refreshToken, long expiresIn) {
}