package com.khaleo.flashcard.controller.deck;

import com.khaleo.flashcard.controller.deck.dto.ImportResponse;
import com.khaleo.flashcard.controller.deck.dto.PublicDeckDetailResponse;
import com.khaleo.flashcard.controller.deck.dto.PublicDeckPageResponse;
import com.khaleo.flashcard.service.auth.VerifiedAccountGuard;
import com.khaleo.flashcard.service.deck.DeckAuthorizationService;
import com.khaleo.flashcard.service.deck.PublicDeckDiscoveryService;
import com.khaleo.flashcard.service.importmerge.PublicDeckImportService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public/decks")
@RequiredArgsConstructor
public class PublicDeckController {

    private final PublicDeckDiscoveryService publicDeckDiscoveryService;
    private final PublicDeckImportService publicDeckImportService;
    private final DeckAuthorizationService deckAuthorizationService;
    private final VerifiedAccountGuard verifiedAccountGuard;

    @GetMapping
    public PublicDeckPageResponse listPublicDecks(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        var deckPage = publicDeckDiscoveryService.listPublicDecks(q, page, size);
        return new PublicDeckPageResponse(
                deckPage.getContent(),
                deckPage.getNumber(),
                deckPage.getSize(),
                deckPage.getTotalElements(),
                deckPage.getTotalPages());
    }

    @GetMapping("/{deckId}")
    public PublicDeckDetailResponse getPublicDeck(@PathVariable("deckId") UUID deckId) {
        return publicDeckDiscoveryService.getPublicDeckDetail(deckId);
    }

    @PostMapping("/{deckId}/import")
    @ResponseStatus(HttpStatus.CREATED)
    public ImportResponse importDeck(@PathVariable("deckId") UUID deckId) {
        UUID actorId = deckAuthorizationService.requireActorId("create", "import", deckId.toString());
        verifiedAccountGuard.requireVerified(actorId, "create", "import", deckId.toString());
        var result = publicDeckImportService.importPublicDeck(actorId, deckId);
        return new ImportResponse(
                result.importLinkId(),
                result.sourceDeckId(),
                result.targetPrivateDeckId(),
                result.status(),
                result.conflictsCount());
    }
}
