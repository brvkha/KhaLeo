package com.khaleo.flashcard.entity;

import com.khaleo.flashcard.entity.enums.UserRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
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
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_users_email", columnNames = "email")
        })
public class User extends BaseAuditableEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", nullable = false, updatable = false, columnDefinition = "char(36)")
    private UUID id;

    @Email
    @NotBlank
    @Column(name = "email", nullable = false, length = 320)
    private String email;

    @NotBlank
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 32)
    private UserRole role;

    @Builder.Default
    @Column(name = "is_email_verified", nullable = false)
    private Boolean isEmailVerified = Boolean.FALSE;

    @Min(1)
    @Max(9999)
    @Builder.Default
    @Column(name = "daily_learning_limit", nullable = false)
    private Integer dailyLearningLimit = 9999;

    @Builder.Default
    @OneToMany(mappedBy = "author", fetch = FetchType.LAZY)
    private List<Deck> decks = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<CardLearningState> learningStates = new ArrayList<>();

    @Builder.Default
    @Column(name = "failed_login_attempts", nullable = false)
    private Integer failedLoginAttempts = 0;

    @Column(name = "account_locked_until")
    private Instant accountLockedUntil;

    @Column(name = "banned_at")
    private Instant bannedAt;

    @Column(name = "banned_by", columnDefinition = "char(36)")
    private UUID bannedBy;

    @Builder.Default
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<RefreshToken> refreshTokens = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<EmailVerificationToken> emailVerificationTokens = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<PasswordResetToken> passwordResetTokens = new ArrayList<>();

    @PrePersist
    @PreUpdate
    void applyDefaults() {
        normalizeEmail();

        if (role == null) {
            role = UserRole.ROLE_USER;
        }
        if (isEmailVerified == null) {
            isEmailVerified = Boolean.FALSE;
        }
        if (dailyLearningLimit == null) {
            dailyLearningLimit = 9999;
        }
        if (failedLoginAttempts == null) {
            failedLoginAttempts = 0;
        }
        if (failedLoginAttempts < 0) {
            throw new IllegalStateException("failedLoginAttempts must be non-negative.");
        }
        if (dailyLearningLimit < 1 || dailyLearningLimit > 9999) {
            throw new IllegalStateException("dailyLearningLimit must be between 1 and 9999.");
        }
    }

    private void normalizeEmail() {
        if (email != null) {
            email = email.trim().toLowerCase();
        }
    }
}
