package com.khaleo.flashcard.entity;

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
@Table(name = "media_upload_authorizations")
public class MediaUploadAuthorization extends BaseAuditableEntity {

    public enum AuthorizationStatus {
        ISSUED,
        EXPIRED,
        REJECTED_TYPE,
        REJECTED_SIZE,
        REJECTED_RATE_LIMIT
    }

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", nullable = false, updatable = false, columnDefinition = "char(36)")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "object_key", nullable = false, length = 512)
    private String objectKey;

    @Column(name = "content_type", nullable = false, length = 64)
    private String contentType;

    @Column(name = "max_size_bytes", nullable = false)
    private Long maxSizeBytes;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private AuthorizationStatus status;

    @Column(name = "rejection_reason", length = 128)
    private String rejectionReason;

    @PrePersist
    void applyDefaults() {
        if (status == null) {
            status = AuthorizationStatus.ISSUED;
        }
        if (issuedAt == null) {
            issuedAt = Instant.now();
        }
        if (maxSizeBytes == null || maxSizeBytes <= 0) {
            throw new IllegalStateException("maxSizeBytes must be greater than 0.");
        }
    }
}
