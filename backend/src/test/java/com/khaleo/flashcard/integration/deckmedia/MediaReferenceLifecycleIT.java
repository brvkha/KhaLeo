package com.khaleo.flashcard.integration.deckmedia;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.khaleo.flashcard.entity.Card;
import com.khaleo.flashcard.entity.Deck;
import com.khaleo.flashcard.entity.User;
import com.khaleo.flashcard.entity.enums.UserRole;
import com.khaleo.flashcard.integration.support.IntegrationPersistenceTestBase;
import com.khaleo.flashcard.repository.MediaObjectReferenceRepository;
import com.khaleo.flashcard.repository.UserRepository;
import com.khaleo.flashcard.service.persistence.RelationalPersistenceService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@SpringBootTest
@Transactional
@SuppressWarnings("null")
class MediaReferenceLifecycleIT extends IntegrationPersistenceTestBase {

    @Autowired
    private RelationalPersistenceService service;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MediaObjectReferenceRepository mediaObjectReferenceRepository;

    @MockBean
    private S3Client s3Client;

    @MockBean
    private S3Presigner s3Presigner;

    @Test
    void shouldDeleteFromStorageOnlyWhenReferenceCountReachesZero() {
        User owner = saveUser("it-media-ref-owner@example.com");
        authenticateAs(owner.getId());

        Deck deck = service.createDeck(owner.getId(), new RelationalPersistenceService.CreateDeckRequest("Deck", null, null, false, null));
        Card first = service.createCard(deck.getId(), new RelationalPersistenceService.CreateCardRequest("A", "uploads/shared.mp3", "1", null));
        Card second = service.createCard(deck.getId(), new RelationalPersistenceService.CreateCardRequest("B", "uploads/shared.mp3", "2", null));

        service.updateCard(first.getId(), new RelationalPersistenceService.UpdateCardRequest(null, "uploads/new.mp3", null, null));
        verify(s3Client, times(0)).deleteObject(any(DeleteObjectRequest.class));

        service.deleteCard(second.getId());
        verify(s3Client, times(1)).deleteObject(any(DeleteObjectRequest.class));

        org.assertj.core.api.Assertions.assertThat(mediaObjectReferenceRepository.findById("uploads/shared.mp3")).isPresent();
        org.assertj.core.api.Assertions.assertThat(mediaObjectReferenceRepository.findById("uploads/shared.mp3").get().getReferenceCount()).isEqualTo(0);

        SecurityContextHolder.clearContext();
    }

    private User saveUser(String email) {
        return userRepository.saveAndFlush(User.builder()
                .email(email)
                .passwordHash("hash")
                .role(UserRole.ROLE_USER)
                .isEmailVerified(true)
                .build());
    }

    private void authenticateAs(UUID userId) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId.toString(), null, java.util.Collections.emptyList()));
    }
}
