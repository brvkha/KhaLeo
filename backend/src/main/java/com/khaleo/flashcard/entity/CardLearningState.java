package com.khaleo.flashcard.entity;

import com.khaleo.flashcard.entity.enums.CardLearningStateType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import jakarta.validation.constraints.Min;
import java.math.BigDecimal;
import java.time.Instant;
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
@Table(
        name = "card_learning_states",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_learning_state_user_card", columnNames = {"user_id", "card_id"})
        })
public class CardLearningState extends BaseAuditableEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", nullable = false, updatable = false, columnDefinition = "char(36)")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "card_id", nullable = false)
    private Card card;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false, length = 16)
    private CardLearningStateType state = CardLearningStateType.NEW;

    @Builder.Default
    @Column(name = "ease_factor", nullable = false, precision = 5, scale = 2)
    private BigDecimal easeFactor = BigDecimal.valueOf(2.5);

    @Builder.Default
    @Min(0)
    @Column(name = "interval_in_days", nullable = false)
    private Integer intervalInDays = 0;

    @Column(name = "next_review_date")
    private Instant nextReviewDate;

    @Column(name = "last_reviewed_at")
    private Instant lastReviewedAt;

    @Builder.Default
    @Min(0)
    @Column(name = "learning_step_good_count", nullable = false)
    private Integer learningStepGoodCount = 0;

    @Builder.Default
    @Column(name = "fsrs_stability", nullable = false, precision = 10, scale = 4)
    private BigDecimal fsrsStability = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "fsrs_difficulty", nullable = false, precision = 5, scale = 2)
    private BigDecimal fsrsDifficulty = BigDecimal.ZERO;

    @Builder.Default
    @Min(0)
    @Column(name = "fsrs_elapsed_days", nullable = false)
    private Integer fsrsElapsedDays = 0;

    @Builder.Default
    @Min(0)
    @Column(name = "fsrs_scheduled_days", nullable = false)
    private Integer fsrsScheduledDays = 0;

    @Builder.Default
    @Min(0)
    @Column(name = "fsrs_reps", nullable = false)
    private Integer fsrsReps = 0;

    @Builder.Default
    @Min(0)
    @Column(name = "fsrs_lapses", nullable = false)
    private Integer fsrsLapses = 0;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @PrePersist
    void applyDefaults() {
        if (state == null) {
            state = CardLearningStateType.NEW;
        }
        if (easeFactor == null) {
            easeFactor = BigDecimal.valueOf(2.5);
        }
        if (intervalInDays == null) {
            intervalInDays = 0;
        }
        if (learningStepGoodCount == null) {
            learningStepGoodCount = 0;
        }
        if (fsrsStability == null) {
            fsrsStability = BigDecimal.ZERO;
        }
        if (fsrsDifficulty == null) {
            fsrsDifficulty = BigDecimal.ZERO;
        }
        if (fsrsElapsedDays == null) {
            fsrsElapsedDays = 0;
        }
        if (fsrsScheduledDays == null) {
            fsrsScheduledDays = 0;
        }
        if (fsrsReps == null) {
            fsrsReps = 0;
        }
        if (fsrsLapses == null) {
            fsrsLapses = 0;
        }
    }
}
