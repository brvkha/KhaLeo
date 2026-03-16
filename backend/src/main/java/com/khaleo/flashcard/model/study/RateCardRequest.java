package com.khaleo.flashcard.model.study;

import com.khaleo.flashcard.model.dynamo.RatingGiven;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record RateCardRequest(
        @NotNull RatingGiven rating,
        @NotNull @Min(0) Long timeSpentMs) {
}
