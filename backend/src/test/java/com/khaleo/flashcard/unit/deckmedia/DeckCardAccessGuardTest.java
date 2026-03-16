package com.khaleo.flashcard.unit.deckmedia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.khaleo.flashcard.entity.Deck;
import com.khaleo.flashcard.entity.User;
import com.khaleo.flashcard.entity.enums.UserRole;
import com.khaleo.flashcard.repository.UserRepository;
import com.khaleo.flashcard.service.persistence.DeckCardAccessGuard;
import com.khaleo.flashcard.service.persistence.PersistenceValidationException;
import com.khaleo.flashcard.service.persistence.PersistenceValidationExceptionMapper;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class DeckCardAccessGuardTest {

    @Mock
    private UserRepository userRepository;

    private final PersistenceValidationExceptionMapper exceptionMapper = new PersistenceValidationExceptionMapper();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldExtractCurrentUserIdFromSecurityContext() {
        DeckCardAccessGuard guard = new DeckCardAccessGuard(userRepository, exceptionMapper);
        UUID userId = UUID.randomUUID();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId.toString(), null, java.util.Collections.emptyList()));

        assertThat(guard.currentUserId()).contains(userId);
    }

    @Test
    void shouldAllowReadForPublicDeckWithoutAuth() {
        DeckCardAccessGuard guard = new DeckCardAccessGuard(userRepository, exceptionMapper);
        Deck deck = Deck.builder().id(UUID.randomUUID()).isPublic(true).author(User.builder().id(UUID.randomUUID()).build()).build();

        guard.ensureCanReadDeck(deck);
    }

    @Test
    void shouldRejectPrivateReadWithoutAuth() {
        DeckCardAccessGuard guard = new DeckCardAccessGuard(userRepository, exceptionMapper);
        Deck deck = Deck.builder().id(UUID.randomUUID()).isPublic(false).author(User.builder().id(UUID.randomUUID()).build()).build();

        assertThatThrownBy(() -> guard.ensureCanReadDeck(deck))
                .isInstanceOf(PersistenceValidationException.class);
    }

    @Test
    void shouldAllowAdminForOwnerGuard() {
        DeckCardAccessGuard guard = new DeckCardAccessGuard(userRepository, exceptionMapper);
        UUID adminId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(adminId.toString(), null, java.util.Collections.emptyList()));
        when(userRepository.findById(adminId)).thenReturn(Optional.of(User.builder().id(adminId).role(UserRole.ROLE_ADMIN).build()));

        guard.ensureOwnerOrAdmin(ownerId, "delete", "deck", ownerId.toString());
    }
}
