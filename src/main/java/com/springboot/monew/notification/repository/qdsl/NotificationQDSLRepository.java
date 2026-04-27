package com.springboot.monew.notification.repository.qdsl;

import com.springboot.monew.notification.entity.Notification;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

public interface NotificationQDSLRepository {

  Slice<Notification> findByCursor(
      UUID cursor,
      Instant after,
      UUID userId,
      Pageable pageable);
}
