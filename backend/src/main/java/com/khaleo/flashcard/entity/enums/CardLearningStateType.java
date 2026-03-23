package com.khaleo.flashcard.entity.enums;

public enum CardLearningStateType {
    NEW,
    LEARNING,
    RELEARNING,
    // Legacy compatibility for historical rows created before FSRS migration.
    MASTERED,
    REVIEW
}
