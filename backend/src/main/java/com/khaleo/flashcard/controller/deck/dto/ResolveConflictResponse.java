package com.khaleo.flashcard.controller.deck.dto;

import com.khaleo.flashcard.entity.enums.ReimportConflictResolutionChoice;
import java.util.UUID;

public record ResolveConflictResponse(
        UUID importLinkId,
        UUID conflictId,
        ReimportConflictResolutionChoice choice,
        int remainingConflicts) {
}
