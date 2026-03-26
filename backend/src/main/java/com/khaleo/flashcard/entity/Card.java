package com.khaleo.flashcard.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "cards")
public class Card extends BaseAuditableEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", nullable = false, updatable = false, columnDefinition = "char(36)")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "deck_id", nullable = false)
    private Deck deck;

    @Column(name = "front_text", columnDefinition = "text")
    private String frontText;

    @Column(name = "front_media_url", length = 2048)
    private String frontMediaUrl;

    @Column(name = "back_text", columnDefinition = "text")
    private String backText;

    @Column(name = "back_media_url", length = 2048)
    private String backMediaUrl;

    @Column(name = "image_url", length = 2048)
    private String imageUrl;

    @Column(name = "part_of_speech", length = 64)
    private String partOfSpeech;

    @Column(name = "phonetic", length = 255)
    private String phonetic;

    @Builder.Default
    @Column(name = "examples_json", nullable = false, columnDefinition = "text")
    private String examplesJson = "[]";

    @Version
    @Builder.Default
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    @Builder.Default
    @OneToMany(mappedBy = "card", fetch = FetchType.LAZY)
    private List<CardLearningState> learningStates = new ArrayList<>();

    @PrePersist
    @PreUpdate
    void validateSides() {
        frontText = normalize(frontText);
        frontMediaUrl = normalize(frontMediaUrl);
        backText = normalize(backText);
        backMediaUrl = normalize(backMediaUrl);
        imageUrl = normalize(imageUrl);
        partOfSpeech = normalize(partOfSpeech);
        phonetic = normalize(phonetic);
        examplesJson = normalizeExamplesJson(examplesJson);

        boolean hasFrontContent = hasValue(frontText) || hasValue(frontMediaUrl);
        boolean hasBackContent = hasValue(backText) || hasValue(backMediaUrl);

        if (!hasFrontContent || !hasBackContent) {
            throw new IllegalStateException(
                    "Card front and back must each contain text or media content.");
        }
    }

    private boolean hasValue(String value) {
        return value != null && !value.isBlank();
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeExamplesJson(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "[]";
        }
        return value;
    }

    public String getTerm() {
        return frontText;
    }

    public void setTerm(String term) {
        this.frontText = term;
    }

    public String getAnswer() {
        return backText;
    }

    public void setAnswer(String answer) {
        this.backText = answer;
    }
}
