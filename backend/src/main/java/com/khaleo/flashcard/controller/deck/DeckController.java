package com.khaleo.flashcard.controller.deck;

import com.khaleo.flashcard.controller.common.PagedResponse;
import com.khaleo.flashcard.controller.deck.dto.CreateDeckRequest;
import com.khaleo.flashcard.controller.deck.dto.DeckResponse;
import com.khaleo.flashcard.controller.deck.dto.UpdateDeckRequest;
import com.khaleo.flashcard.entity.Deck;
import com.khaleo.flashcard.service.persistence.DeckCardAccessGuard;
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
@RequestMapping("/api/v1/decks")
@RequiredArgsConstructor
public class DeckController {

    private final RelationalPersistenceService relationalPersistenceService;
    private final DeckCardAccessGuard deckCardAccessGuard;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DeckResponse createDeck(@Valid @RequestBody CreateDeckRequest request) {
        UUID actorId = deckCardAccessGuard.requireAuthenticatedUserId("create", "deck", "new");
        Deck created = relationalPersistenceService.createDeck(
                actorId,
                new RelationalPersistenceService.CreateDeckRequest(
                        request.name(),
                        request.description(),
                        request.coverImageUrl(),
                        request.isPublic(),
                        request.tags()));
        return DeckResponse.from(created);
    }

    @GetMapping
    public PagedResponse<DeckResponse> listDecks(
            @RequestParam(required = false) Boolean isPublic,
            @RequestParam(required = false) UUID authorId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return PagedResponse.from(relationalPersistenceService
                .listDecks(isPublic, authorId, page, size)
                .map(DeckResponse::from));
    }

    @GetMapping("/{id}")
    public DeckResponse getDeck(@PathVariable("id") UUID id) {
        return DeckResponse.from(relationalPersistenceService.getDeck(id));
    }

    @PutMapping("/{id}")
    public DeckResponse updateDeck(@PathVariable("id") UUID id, @Valid @RequestBody UpdateDeckRequest request) {
        Deck updated = relationalPersistenceService.updateDeck(
                id,
                new RelationalPersistenceService.UpdateDeckRequest(
                        request.name(),
                        request.description(),
                        request.coverImageUrl(),
                        request.isPublic(),
                        request.tags()));
        return DeckResponse.from(updated);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDeck(@PathVariable("id") UUID id) {
        relationalPersistenceService.deleteDeck(id);
    }
}
