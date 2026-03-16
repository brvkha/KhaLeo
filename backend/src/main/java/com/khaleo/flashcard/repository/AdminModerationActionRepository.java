package com.khaleo.flashcard.repository;

import com.khaleo.flashcard.entity.AdminModerationAction;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminModerationActionRepository extends JpaRepository<AdminModerationAction, UUID> {
}
