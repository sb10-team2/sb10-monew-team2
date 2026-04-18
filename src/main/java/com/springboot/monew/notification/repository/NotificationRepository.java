package com.springboot.monew.notification.repository;

import com.springboot.monew.notification.entity.Notification;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

  @Query("SELECT n FROM Notification n " +
      "JOIN FETCH n.user " +
      "LEFT JOIN FETCH n.interest " +
      "LEFT JOIN FETCH n.commentLike " +
      "WHERE n.user.id = :userId " +
      "AND (:lastCreatedAt IS NULL OR n.createdAt < :lastCreatedAt) " +
      "ORDER BY n.createdAt DESC")
  Slice<Notification> findByCursor(
      @Param("userId") UUID userId,
      @Param("lastCreatedAt") Instant lastCreatedAt,
      Pageable pageable);
}
