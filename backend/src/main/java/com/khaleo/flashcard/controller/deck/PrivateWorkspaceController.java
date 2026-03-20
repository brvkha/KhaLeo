package com.khaleo.flashcard.controller.deck;

import com.khaleo.flashcard.controller.card.dto.CardResponse;
import com.khaleo.flashcard.controller.card.dto.CardSearchQuery;
import com.khaleo.flashcard.controller.common.PagedResponse;
import com.khaleo.flashcard.controller.deck.dto.CreateDeckRequest;
import com.khaleo.flashcard.controller.deck.dto.DeckResponse;
import com.khaleo.flashcard.controller.deck.dto.UpdateDeckRequest;
import com.khaleo.flashcard.entity.Deck;
import com.khaleo.flashcard.service.auth.VerifiedAccountGuard;
import com.khaleo.flashcard.service.deck.DeckAuthorizationService;
import com.khaleo.flashcard.service.deck.PrivateDeckCrudService;
import com.khaleo.flashcard.service.deck.PrivateWorkspaceService;
import com.khaleo.flashcard.service.persistence.RelationalPersistenceService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/private/decks")
@RequiredArgsConstructor
public class PrivateWorkspaceController {

    private final DeckAuthorizationService deckAuthorizationService;
    private final VerifiedAccountGuard verifiedAccountGuard;
    private final PrivateWorkspaceService privateWorkspaceService;
    private final PrivateDeckCrudService privateDeckCrudService;

    @GetMapping
    public PagedResponse<DeckResponse> listPrivateDecks(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        UUID actorId = deckAuthorizationService.requireActorId("list", "deck", "private");
        verifiedAccountGuard.requireVerified(actorId, "list", "deck", "private");
        return PagedResponse.from(privateWorkspaceService
                .listOwnedPrivateDecks(actorId, q, page, size)
                .map(DeckResponse::from));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DeckResponse createPrivateDeck(@Valid @RequestBody CreateDeckRequest request) {
        UUID actorId = deckAuthorizationService.requireActorId("create", "deck", "private");
        verifiedAccountGuard.requireVerified(actorId, "create", "deck", "private");
        Deck created = privateDeckCrudService.createPrivateDeck(
                actorId,
                new RelationalPersistenceService.CreateDeckRequest(
                        request.name(),
                        request.description(),
                        request.coverImageUrl(),
                        false,
                        request.tags()));
        return DeckResponse.from(created);
    }

    @PutMapping("/{id}")
    public DeckResponse updatePrivateDeck(@PathVariable("id") UUID id, @Valid @RequestBody UpdateDeckRequest request) {
        UUID actorId = deckAuthorizationService.requireActorId("update", "deck", id.toString());
        verifiedAccountGuard.requireVerified(actorId, "update", "deck", id.toString());
        Deck updated = privateDeckCrudService.updatePrivateDeck(
                actorId,
                id,
                new RelationalPersistenceService.UpdateDeckRequest(
                        request.name(),
                        request.description(),
                        request.coverImageUrl(),
                        false,
                        request.tags()));
        return DeckResponse.from(updated);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePrivateDeck(@PathVariable("id") UUID id) {
        UUID actorId = deckAuthorizationService.requireActorId("delete", "deck", id.toString());
        verifiedAccountGuard.requireVerified(actorId, "delete", "deck", id.toString());
        privateDeckCrudService.deletePrivateDeck(actorId, id);
    }

    @GetMapping("/{deckId}/cards/search")
    public PagedResponse<CardResponse> searchCards(
            @PathVariable("deckId") UUID deckId,
            @RequestParam(required = false) String frontText,
            @RequestParam(required = false) String backText,
            @RequestParam(required = false) String vocabulary,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        UUID actorId = deckAuthorizationService.requireActorId("search", "card", deckId.toString());
        verifiedAccountGuard.requireVerified(actorId, "search", "card", deckId.toString());
        CardSearchQuery query = new CardSearchQuery(frontText, backText, vocabulary, page, size).normalized();
        return PagedResponse.from(privateDeckCrudService
                .searchCards(actorId, deckId, new RelationalPersistenceService.CardSearchQuery(
                        query.frontText(),
                        query.backText(),
                        query.vocabulary(),
                        query.page(),
                        query.size()))
                .map(CardResponse::from));
    }
}
