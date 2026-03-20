package com.khaleo.flashcard.entity;

import com.khaleo.flashcard.entity.enums.ReimportConflictResolutionChoice;
import com.khaleo.flashcard.entity.enums.ReimportConflictScope;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
@Table(name = "reimport_merge_conflicts")
public class ReimportMergeConflict extends BaseAuditableEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", nullable = false, updatable = false, columnDefinition = "char(36)")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "deck_import_link_id", nullable = false)
    private DeckImportLink deckImportLink;

    @Enumerated(EnumType.STRING)
    @Column(name = "conflict_scope", nullable = false, length = 32)
    private ReimportConflictScope conflictScope;

    @Column(name = "target_entity_id", nullable = false, length = 64)
    private String targetEntityId;

    @Column(name = "field_path", nullable = false, length = 255)
    private String fieldPath;

    @Column(name = "local_value_snapshot", columnDefinition = "text")
    private String localValueSnapshot;

    @Column(name = "cloud_value_snapshot", columnDefinition = "text")
    private String cloudValueSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(name = "resolution_choice", length = 16)
    private ReimportConflictResolutionChoice resolutionChoice;

    @Column(name = "resolved_at")
    private Instant resolvedAt;
}
