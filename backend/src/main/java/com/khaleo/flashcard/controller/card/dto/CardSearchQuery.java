package com.khaleo.flashcard.controller.card.dto;

public record CardSearchQuery(
        String frontText,
        String backText,
        String vocabulary,
        Integer page,
        Integer size) {

    public CardSearchQuery normalized() {
        return new CardSearchQuery(
                normalize(frontText),
                normalize(backText),
                normalize(vocabulary),
                page,
                size);
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
