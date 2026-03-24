package com.khaleo.flashcard.repository;

import com.khaleo.flashcard.entity.AdminModerationAction;
import com.khaleo.flashcard.entity.enums.AdminActionStatus;
import com.khaleo.flashcard.entity.enums.AdminTargetType;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AdminModerationActionRepository extends JpaRepository<AdminModerationAction, UUID> {

	@Query("""
			select a from AdminModerationAction a
			left join User u on u.id = a.adminUserId
			where (:targetType is null or a.targetType = :targetType)
				and (:status is null or a.status = :status)
				and (:adminUserId is null or a.adminUserId = :adminUserId)
				and (:adminEmail is null or lower(coalesce(u.email, '')) like lower(concat('%', :adminEmail, '%')))
			""")
	Page<AdminModerationAction> searchForAdmin(
			@Param("adminUserId") UUID adminUserId,
			@Param("adminEmail") String adminEmail,
			@Param("targetType") AdminTargetType targetType,
			@Param("status") AdminActionStatus status,
			Pageable pageable);
}
