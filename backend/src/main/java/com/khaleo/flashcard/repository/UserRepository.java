package com.khaleo.flashcard.repository;

import com.khaleo.flashcard.entity.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("""
            select u from User u
            where (:queryText is null or lower(coalesce(u.email, '')) like lower(concat('%', :queryText, '%')))
            """)
    Page<User> searchForAdmin(@Param("queryText") String queryText, Pageable pageable);
}
