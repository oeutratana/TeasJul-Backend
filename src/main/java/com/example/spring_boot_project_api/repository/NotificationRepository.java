package com.example.spring_boot_project_api.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.spring_boot_project_api.model.Notification;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    long countByUserIdAndReadAtIsNull(Long userId);

            boolean existsByUserIdAndTypeAndReferenceTypeAndReferenceId(Long userId,
              com.example.spring_boot_project_api.enums.NotificationType type,
              String referenceType, Long referenceId);

    /**
     * Marks every unread notification of the given user as read. The user id is
     * always derived from the authenticated principal, never from the request.
     */
    @Modifying
    @Query("""
            UPDATE Notification n
               SET n.readAt = CURRENT_TIMESTAMP
             WHERE n.user.id = :userId
               AND n.readAt IS NULL
            """)
    int markAllAsRead(@Param("userId") Long userId);
}