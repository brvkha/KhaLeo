package com.khaleo.flashcard.controller.study;

import com.khaleo.flashcard.model.study.NextCardsPageResponse;
import com.khaleo.flashcard.model.study.NextCardsRequest;
import com.khaleo.flashcard.model.study.AlgorithmWeightsRequest;
import com.khaleo.flashcard.model.study.AlgorithmWeightsResponse;
import com.khaleo.flashcard.model.study.RateCardRequest;
import com.khaleo.flashcard.model.study.RateCardPreviewResponse;
import com.khaleo.flashcard.model.study.RateCardResponse;
import com.khaleo.flashcard.service.auth.VerifiedAccountGuard;
import com.khaleo.flashcard.service.persistence.DeckCardAccessGuard;
import com.khaleo.flashcard.service.study.NextCardsService;
import com.khaleo.flashcard.service.study.SpacedRepetitionService;
import com.khaleo.flashcard.service.study.StudyRatingService;
import jakarta.validation.Valid;
import java.util.Arrays;
import java.util.List;
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
    private final SpacedRepetitionService spacedRepetitionService;

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
        // Rich-card projection fields are read/display concerns; rating remains on the existing FSRS path.
        return studyRatingService.rateCard(cardId, request);
    }

    @GetMapping("/cards/{cardId}/preview-ratings")
    public RateCardPreviewResponse previewCardRatings(@PathVariable("cardId") UUID cardId) {
        UUID actorId = deckCardAccessGuard.requireAuthenticatedUserId("preview", "study-session", cardId.toString());
        verifiedAccountGuard.requireVerified(actorId, "preview", "study-session", cardId.toString());
        return studyRatingService.previewCardRatings(cardId);
    }

    @GetMapping("/algorithm-weights")
    public AlgorithmWeightsResponse getAlgorithmWeights() {
        UUID actorId = deckCardAccessGuard.requireAuthenticatedUserId("read", "study-settings", "algorithm-weights");
        deckCardAccessGuard.ensureAdmin(actorId, "read", "study-settings", "algorithm-weights");
        verifiedAccountGuard.requireVerified(actorId, "read", "study-settings", "algorithm-weights");
        return new AlgorithmWeightsResponse(toList(spacedRepetitionService.getWeights()));
    }

    @PostMapping("/algorithm-weights")
    public AlgorithmWeightsResponse updateAlgorithmWeights(
            @Valid @RequestBody AlgorithmWeightsRequest request) {
        UUID actorId = deckCardAccessGuard.requireAuthenticatedUserId("update", "study-settings", "algorithm-weights");
        deckCardAccessGuard.ensureAdmin(actorId, "update", "study-settings", "algorithm-weights");
        verifiedAccountGuard.requireVerified(actorId, "update", "study-settings", "algorithm-weights");
        double[] updated = spacedRepetitionService.updateWeights(toArray(request.weights()));
        return new AlgorithmWeightsResponse(toList(updated));
    }

    @PostMapping("/algorithm-weights/reset")
    public AlgorithmWeightsResponse resetAlgorithmWeights() {
        UUID actorId = deckCardAccessGuard.requireAuthenticatedUserId("reset", "study-settings", "algorithm-weights");
        deckCardAccessGuard.ensureAdmin(actorId, "reset", "study-settings", "algorithm-weights");
        verifiedAccountGuard.requireVerified(actorId, "reset", "study-settings", "algorithm-weights");
        return new AlgorithmWeightsResponse(toList(spacedRepetitionService.resetWeights()));
    }

    private List<Double> toList(double[] weights) {
        return Arrays.stream(weights).boxed().toList();
    }

    private double[] toArray(List<Double> weights) {
        return weights.stream().mapToDouble(Double::doubleValue).toArray();
    }
}
