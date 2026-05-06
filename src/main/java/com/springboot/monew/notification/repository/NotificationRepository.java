package com.springboot.monew.notification.repository;

import com.springboot.monew.notification.entity.Notification;
import com.springboot.monew.notification.repository.qdsl.NotificationQDSLRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface NotificationRepository extends JpaRepository<Notification, UUID>,
    NotificationQDSLRepository {

  @Modifying(clearAutomatically = true)
  @Query("update Notification n "
      + "set n.confirmed = true, n.updatedAt = :updatedAt "
      + "where n.user.id = :userId and n.confirmed = false")
  int bulkUpdateConfirmed(UUID userId, Instant updatedAt);

  long countAllByUser_IdAndConfirmedIsFalse(UUID userId);

  @Modifying
  @Query(value = "delete from notifications where id in ("
      + "select id "
      + "from notifications "
      + "where confirmed = true and updated_at < :threshold "
      + "limit :limit)",
      nativeQuery = true)
  long deleteOutdatedByChunk(Instant threshold, long limit);

  Optional<Notification> findByIdAndUser_Id(UUID id, UUID userId);
}
