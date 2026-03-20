package com.khaleo.flashcard.controller.study;

import com.khaleo.flashcard.model.study.NextCardsPageResponse;
import com.khaleo.flashcard.model.study.NextCardsRequest;
import com.khaleo.flashcard.model.study.RateCardRequest;
import com.khaleo.flashcard.model.study.RateCardResponse;
import com.khaleo.flashcard.service.auth.VerifiedAccountGuard;
import com.khaleo.flashcard.service.persistence.DeckCardAccessGuard;
import com.khaleo.flashcard.service.study.NextCardsService;
import com.khaleo.flashcard.service.study.StudyRatingService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/study-session")
@RequiredArgsConstructor
public class StudySessionController {

    private final NextCardsService nextCardsService;
    private final StudyRatingService studyRatingService;
    private final DeckCardAccessGuard deckCardAccessGuard;
    private final VerifiedAccountGuard verifiedAccountGuard;

    @GetMapping("/decks/{deckId}/next-cards")
    public NextCardsPageResponse nextCards(
            @PathVariable("deckId") UUID deckId,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String continuationToken) {
        UUID actorId = deckCardAccessGuard.requireAuthenticatedUserId("read", "study-session", deckId.toString());
        verifiedAccountGuard.requireVerified(actorId, "read", "study-session", deckId.toString());
        return nextCardsService.getNextCards(deckId, new NextCardsRequest(size, continuationToken));
    }

    @PostMapping("/cards/{cardId}/rate")
    public RateCardResponse rateCard(
            @PathVariable("cardId") UUID cardId,
            @Valid @RequestBody RateCardRequest request) {
        UUID actorId = deckCardAccessGuard.requireAuthenticatedUserId("rate", "study-session", cardId.toString());
        verifiedAccountGuard.requireVerified(actorId, "rate", "study-session", cardId.toString());
        return studyRatingService.rateCard(cardId, request);
    }
}
