package com.khaleo.flashcard.controller.card;

import com.khaleo.flashcard.controller.card.dto.CardResponse;
import com.khaleo.flashcard.controller.card.dto.CardSearchQuery;
import com.khaleo.flashcard.controller.card.dto.CreateCardRequest;
import com.khaleo.flashcard.controller.card.dto.UpdateCardRequest;
import com.khaleo.flashcard.controller.common.PagedResponse;
import com.khaleo.flashcard.entity.Card;
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
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CardController {

    private final RelationalPersistenceService relationalPersistenceService;

    @PostMapping("/decks/{deckId}/cards")
    @ResponseStatus(HttpStatus.CREATED)
    public CardResponse createCard(
            @PathVariable("deckId") UUID deckId,
            @Valid @RequestBody CreateCardRequest request) {
        Card created = relationalPersistenceService.createCard(
                deckId,
                new RelationalPersistenceService.CreateCardRequest(
                        request.frontText(),
                        request.frontMediaUrl(),
                        request.backText(),
                        request.backMediaUrl()));
        return CardResponse.from(created);
    }

    @GetMapping("/decks/{deckId}/cards/search")
    public PagedResponse<CardResponse> searchCards(
            @PathVariable("deckId") UUID deckId,
            @RequestParam(required = false) String frontText,
            @RequestParam(required = false) String backText,
            @RequestParam(required = false) String vocabulary,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        CardSearchQuery query = new CardSearchQuery(frontText, backText, vocabulary, page, size).normalized();
        return PagedResponse.from(relationalPersistenceService
                .searchCardsInDeck(
                        deckId,
                        new RelationalPersistenceService.CardSearchQuery(
                                query.frontText(),
                                query.backText(),
                                query.vocabulary(),
                                query.page(),
                                query.size()))
                .map(CardResponse::from));
    }

    @PutMapping("/cards/{id}")
    public CardResponse updateCard(@PathVariable("id") UUID id, @Valid @RequestBody UpdateCardRequest request) {
        Card updated = relationalPersistenceService.updateCard(
                id,
                new RelationalPersistenceService.UpdateCardRequest(
                        request.frontText(),
                        request.frontMediaUrl(),
                        request.backText(),
                        request.backMediaUrl()));
        return CardResponse.from(updated);
    }

    @DeleteMapping("/cards/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCard(@PathVariable("id") UUID id) {
        relationalPersistenceService.deleteCard(id);
    }
}
