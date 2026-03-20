package com.khaleo.flashcard.controller.deck.dto;

import com.khaleo.flashcard.entity.enums.ReimportConflictResolutionChoice;
import jakarta.validation.constraints.NotNull;

public record ResolveConflictRequest(
        @NotNull ReimportConflictResolutionChoice choice) {
}
