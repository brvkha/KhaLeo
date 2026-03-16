package com.khaleo.flashcard.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "media_object_references")
public class MediaObjectReference extends BaseAuditableEntity {

    @Id
    @Column(name = "object_key", nullable = false, length = 512)
    private String objectKey;

    @Builder.Default
    @Column(name = "reference_count", nullable = false)
    private Integer referenceCount = 0;

    @Column(name = "last_referenced_at", nullable = false)
    private Instant lastReferencedAt;

    @Column(name = "last_dereferenced_at")
    private Instant lastDereferencedAt;

    @PrePersist
    @PreUpdate
    void validate() {
        if (objectKey == null || objectKey.isBlank()) {
            throw new IllegalStateException("objectKey must not be blank.");
        }
        if (referenceCount == null || referenceCount < 0) {
            throw new IllegalStateException("referenceCount must be non-negative.");
        }
        if (lastReferencedAt == null) {
            lastReferencedAt = Instant.now();
        }
    }
}
