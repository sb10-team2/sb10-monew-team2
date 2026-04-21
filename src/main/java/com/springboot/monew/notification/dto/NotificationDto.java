package com.springboot.monew.notification.dto;

import com.springboot.monew.notification.entity.Notification;
import com.springboot.monew.notification.entity.ResourceType;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

/**
 * DTO for {@link Notification}
 */
@Builder
public record NotificationDto(
    UUID id,
    Instant createdAt,
    Instant updatedAt,
    Boolean confirmed,
    String content,
    ResourceType resourceType,
    UUID userId,
    UUID resourceId)
    implements Serializable {

}
