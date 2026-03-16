package com.khaleo.flashcard.entity;

import com.khaleo.flashcard.entity.enums.AdminActionStatus;
import com.khaleo.flashcard.entity.enums.AdminActionType;
import com.khaleo.flashcard.entity.enums.AdminTargetType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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
@Table(name = "admin_moderation_actions")
public class AdminModerationAction extends BaseAuditableEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", nullable = false, updatable = false, columnDefinition = "char(36)")
    private UUID id;

    @Column(name = "admin_user_id", nullable = false, columnDefinition = "char(36)")
    private UUID adminUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 32)
    private AdminActionType actionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 16)
    private AdminTargetType targetType;

    @Column(name = "target_id", nullable = false, columnDefinition = "char(36)")
    private UUID targetId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private AdminActionStatus status;

    @Column(name = "reason_code", length = 128)
    private String reasonCode;
}
