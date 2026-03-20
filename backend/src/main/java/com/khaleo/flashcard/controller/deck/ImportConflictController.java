package com.khaleo.flashcard.controller.deck;

import com.khaleo.flashcard.controller.deck.dto.ConflictListResponse;
import com.khaleo.flashcard.controller.deck.dto.ReimportResponse;
import com.khaleo.flashcard.controller.deck.dto.ResolveConflictRequest;
import com.khaleo.flashcard.controller.deck.dto.ResolveConflictResponse;
import com.khaleo.flashcard.service.auth.VerifiedAccountGuard;
import com.khaleo.flashcard.service.deck.DeckAuthorizationService;
import com.khaleo.flashcard.service.importmerge.ConflictResolutionService;
import com.khaleo.flashcard.service.importmerge.ReimportMergeService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/private/decks/import-links")
@RequiredArgsConstructor
public class ImportConflictController {

    private final ReimportMergeService reimportMergeService;
    private final ConflictResolutionService conflictResolutionService;
    private final DeckAuthorizationService deckAuthorizationService;
    private final VerifiedAccountGuard verifiedAccountGuard;

    @PostMapping("/{importLinkId}/reimport")
    public ReimportResponse reimport(@PathVariable("importLinkId") UUID importLinkId) {
        UUID actorId = deckAuthorizationService.requireActorId("update", "import-link", importLinkId.toString());
        verifiedAccountGuard.requireVerified(actorId, "update", "import-link", importLinkId.toString());
        var result = reimportMergeService.reimport(actorId, importLinkId);
        return new ReimportResponse(result.importLinkId(), result.status(), result.conflictsCount());
    }

    @GetMapping("/{importLinkId}/conflicts")
    public ConflictListResponse listConflicts(@PathVariable("importLinkId") UUID importLinkId) {
        UUID actorId = deckAuthorizationService.requireActorId("read", "import-link", importLinkId.toString());
        verifiedAccountGuard.requireVerified(actorId, "read", "import-link", importLinkId.toString());
        var conflicts = conflictResolutionService.listUnresolved(actorId, importLinkId);
        List<ConflictListResponse.ConflictItem> items = conflicts.stream()
                .map(conflict -> new ConflictListResponse.ConflictItem(
                        conflict.getId(),
                        conflict.getConflictScope(),
                        conflict.getTargetEntityId(),
                        conflict.getFieldPath(),
                        conflict.getLocalValueSnapshot(),
                        conflict.getCloudValueSnapshot(),
                        conflict.getResolutionChoice()))
                .toList();
        return new ConflictListResponse(importLinkId, items.size(), items);
    }

    @PostMapping("/{importLinkId}/conflicts/{conflictId}/resolve")
    public ResolveConflictResponse resolveConflict(
            @PathVariable("importLinkId") UUID importLinkId,
            @PathVariable("conflictId") UUID conflictId,
            @Valid @RequestBody ResolveConflictRequest request) {
        UUID actorId = deckAuthorizationService.requireActorId("update", "merge-conflict", conflictId.toString());
        verifiedAccountGuard.requireVerified(actorId, "update", "merge-conflict", conflictId.toString());
        var result = conflictResolutionService.resolve(actorId, importLinkId, conflictId, request.choice());
        return new ResolveConflictResponse(
                result.importLinkId(),
                result.conflictId(),
                result.choice(),
                result.remainingConflicts());
    }
}
