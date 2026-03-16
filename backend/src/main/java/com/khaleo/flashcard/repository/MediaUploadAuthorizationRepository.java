package com.khaleo.flashcard.repository;

import com.khaleo.flashcard.entity.MediaUploadAuthorization;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MediaUploadAuthorizationRepository extends JpaRepository<MediaUploadAuthorization, UUID> {

    long countByUserIdAndIssuedAtAfter(UUID userId, Instant issuedAfter);
}
