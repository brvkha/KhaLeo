package com.khaleo.flashcard.model.study;

public record NextCardsRequest(
        Integer size,
        String continuationToken) {

    public int resolvedSize(int defaultSize, int maxSize) {
        int candidate = size == null ? defaultSize : size;
        if (candidate < 1 || candidate > maxSize) {
            throw new IllegalArgumentException("Invalid pagination request page size=" + candidate);
        }
        return candidate;
    }

    public StudyPaginationToken resolvedToken() {
        return StudyPaginationToken.from(continuationToken);
    }
}
