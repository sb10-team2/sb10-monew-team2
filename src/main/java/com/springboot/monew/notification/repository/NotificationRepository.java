package com.springboot.monew.notification.repository;

import com.springboot.monew.notification.entity.Notification;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

  @Query("SELECT n FROM Notification n " +
      "JOIN FETCH n.user " +
      "LEFT JOIN FETCH n.interest " +
      "LEFT JOIN FETCH n.commentLike " +
      "WHERE n.user.id = :userId " +
      "AND n.confirmed = false " +
      "AND (:after IS NULL OR "
      + "n.createdAt < :after OR "
      + "(n.createdAt = :after AND n.id > :cursor)) " +
      "ORDER BY n.createdAt DESC, n.id ASC")
  Slice<Notification> findByCursor(
      UUID cursor, @Param("after") Instant after, @Param("userId") UUID userId,
      Pageable pageable);

  @Modifying(clearAutomatically = true)
  @Query("update Notification n "
      + "set n.confirmed = true, n.updatedAt = :updatedAt "
      + "where n.id = :id and n.user.id = :userId")
  int updateConfirmed(UUID id, UUID userId, Instant updatedAt);

  @Modifying(clearAutomatically = true)
  @Query("update Notification n "
      + "set n.confirmed = true, n.updatedAt = :updatedAt "
      + "where n.id in :ids and n.user.id = :userId")
  int updateConfirmed(List<UUID> ids, UUID userId, Instant updatedAt);
}
