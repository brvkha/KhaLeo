package com.khaleo.flashcard.model.study;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record AlgorithmWeightsRequest(
        @NotNull
        @Size(min = 19, max = 19)
        List<Double> weights) {
}
