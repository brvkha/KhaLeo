package com.khaleo.flashcard.controller.deck.dto;

import com.khaleo.flashcard.entity.enums.ImportMergeStatus;
import java.util.UUID;

public record ReimportResponse(
        UUID importLinkId,
        ImportMergeStatus status,
        int conflictsCount) {
}
