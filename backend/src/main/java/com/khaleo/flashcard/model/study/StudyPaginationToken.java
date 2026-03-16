package com.khaleo.flashcard.model.study;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public record StudyPaginationToken(int offset) {

    public static final String PREFIX = "offset:";

    public static StudyPaginationToken initial() {
        return new StudyPaginationToken(0);
    }

    public static StudyPaginationToken from(String encodedToken) {
        if (encodedToken == null || encodedToken.isBlank()) {
            return initial();
        }

        try {
            String decoded = new String(Base64.getUrlDecoder().decode(encodedToken), StandardCharsets.UTF_8);
            if (!decoded.startsWith(PREFIX)) {
                throw new IllegalArgumentException("Invalid continuation token.");
            }
            int parsed = Integer.parseInt(decoded.substring(PREFIX.length()));
            if (parsed < 0) {
                throw new IllegalArgumentException("Continuation token offset must be non-negative.");
            }
            return new StudyPaginationToken(parsed);
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("Invalid continuation token.", ex);
        }
    }

    public String encode() {
        String payload = PREFIX + offset;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
    }
}
