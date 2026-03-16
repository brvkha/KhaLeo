package com.khaleo.flashcard.service.persistence;

import com.khaleo.flashcard.entity.Deck;
import com.khaleo.flashcard.entity.User;
import com.khaleo.flashcard.entity.enums.UserRole;
import com.khaleo.flashcard.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DeckCardAccessGuard {

    private final UserRepository userRepository;
    private final PersistenceValidationExceptionMapper exceptionMapper;

    public Optional<UUID> currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof String principalString) || "anonymousUser".equals(principalString)) {
            return Optional.empty();
        }

        try {
            return Optional.of(UUID.fromString(principalString));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    public UUID requireAuthenticatedUserId(String operation, String resourceType, String resourceKey) {
        UUID userId = currentUserId().orElseThrow(
                () -> exceptionMapper.authorizationDenied(operation, resourceType, resourceKey));
        ensureUserNotBanned(userId, operation, resourceType, resourceKey);
        return userId;
    }

    public void ensureUserNotBanned(UUID userId, String operation, String resourceType, String resourceKey) {
        User user = userRepository.findById(userId).orElse(null);
        if (user != null && user.getBannedAt() != null) {
            throw exceptionMapper.bannedUserDenied(userId, operation, resourceType, resourceKey);
        }
    }

    public void ensureCanReadDeck(Deck deck) {
        if (Boolean.TRUE.equals(deck.getIsPublic())) {
            return;
        }

        UUID userId = requireAuthenticatedUserId("read", "deck", deck.getId().toString());
        if (isOwnerOrAdmin(userId, deck.getAuthor().getId())) {
            return;
        }

        throw exceptionMapper.authorizationDenied("read", "deck", deck.getId().toString());
    }

    public void ensureOwnerOrAdmin(UUID ownerId, String operation, String resourceType, String resourceKey) {
        UUID userId = requireAuthenticatedUserId(operation, resourceType, resourceKey);
        if (!isOwnerOrAdmin(userId, ownerId)) {
            throw exceptionMapper.authorizationDenied(operation, resourceType, resourceKey);
        }
    }

    public void ensureCanViewAuthor(UUID authorId) {
        UUID userId = requireAuthenticatedUserId("list", "deck", authorId.toString());
        if (!isOwnerOrAdmin(userId, authorId)) {
            throw exceptionMapper.authorizationDenied("list", "deck", authorId.toString());
        }
    }

    public boolean isOwnerOrAdmin(UUID actorId, UUID ownerId) {
        if (actorId.equals(ownerId)) {
            return true;
        }

        User user = userRepository.findById(actorId).orElse(null);
        return user != null && user.getRole() == UserRole.ROLE_ADMIN;
    }
}
