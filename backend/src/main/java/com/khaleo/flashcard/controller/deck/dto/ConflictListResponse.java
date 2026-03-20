package com.khaleo.flashcard.controller.deck.dto;

import com.khaleo.flashcard.entity.enums.ReimportConflictResolutionChoice;
import com.khaleo.flashcard.entity.enums.ReimportConflictScope;
import java.util.List;
import java.util.UUID;

public record ConflictListResponse(
        UUID importLinkId,
        int unresolvedCount,
        List<ConflictItem> items) {

    public record ConflictItem(
            UUID conflictId,
            ReimportConflictScope scope,
            String targetEntityId,
            String fieldPath,
            String localValue,
            String cloudValue,
            ReimportConflictResolutionChoice choice) {
    }
}
