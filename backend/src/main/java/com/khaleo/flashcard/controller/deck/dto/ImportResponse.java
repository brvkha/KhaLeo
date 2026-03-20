package com.khaleo.flashcard.controller.deck.dto;

import com.khaleo.flashcard.entity.enums.ImportMergeStatus;
import java.util.UUID;

public record ImportResponse(
        UUID importLinkId,
        UUID sourceDeckId,
        UUID targetPrivateDeckId,
        ImportMergeStatus status,
        int conflictsCount) {
}
