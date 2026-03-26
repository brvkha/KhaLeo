package com.khaleo.flashcard.controller.card.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CreateCardRequest(
        @NotBlank @JsonAlias("frontText") String term,
        @NotBlank @JsonAlias("backText") String answer,
        @Size(max = 2048) @JsonAlias({"frontMediaUrl", "backMediaUrl"}) String imageUrl,
        @Size(max = 64) String partOfSpeech,
        @Size(max = 255) String phonetic,
        @Size(max = 20) List<String> examples) {
}
