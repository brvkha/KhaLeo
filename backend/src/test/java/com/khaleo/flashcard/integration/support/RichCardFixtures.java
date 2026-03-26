package com.khaleo.flashcard.integration.support;

import java.util.List;

public final class RichCardFixtures {

    private RichCardFixtures() {
    }

    public static String validImageUrl() {
        return "https://images.unsplash.com/photo-1";
    }

    public static List<String> validExamples() {
        return List.of("Example one", "Example two");
    }

    public static String longExample(int length) {
        return "x".repeat(Math.max(0, length));
    }
}
