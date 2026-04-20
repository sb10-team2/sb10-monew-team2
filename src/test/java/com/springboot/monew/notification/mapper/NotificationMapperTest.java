package com.springboot.monew.notification.mapper;

import static org.instancio.Select.field;

import com.springboot.monew.notification.dto.NotificationDto;
import com.springboot.monew.notification.entity.Notification;
import com.springboot.monew.notification.entity.ResourceType;
import org.assertj.core.api.Assertions;
import org.instancio.Instancio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class NotificationMapperTest {

  private final NotificationMapper mapper = new NotificationMapperImpl();

  @Test
  @DisplayName("관심사를 참조하는 알람 객체 dto 매핑 성공")
  void notificationWithInterestIsEqualToDto() {
    // given
    Notification notification = Instancio.of(Notification.class)
        .ignore(field(Notification::getCommentLike))
        .set(field(Notification::getResourceType), ResourceType.INTEREST)
        .create();

    // when
    NotificationDto dto = mapper.toDto(notification);

    // then
    Assertions.assertThat(dto)
        .extracting("id", "createdAt", "updatedAt", "confirmed", "content", "userId",
            "resourceType", "resourceId")
        .contains(notification.getId(), notification.getCreatedAt(), notification.getUpdatedAt(),
            notification.getConfirmed(), notification.getContent(), notification.getUser().getId(),
            notification.getResourceType(), notification.getResourceId());
  }

  @Test
  @DisplayName("댓글 좋아요를 참조하는 알람 객체 dto 매핑 성공")
  void notificationWithCommentLikeIsEqualToDto() {
    // given
    Notification notification = Instancio.of(Notification.class)
        .ignore(field(Notification::getInterest))
        .set(field(Notification::getResourceType), ResourceType.COMMENT)
        .create();

    // when
    NotificationDto dto = mapper.toDto(notification);

    // then
    Assertions.assertThat(dto)
        .extracting("id", "createdAt", "updatedAt", "confirmed", "content", "userId",
            "resourceType", "resourceId")
        .contains(notification.getId(), notification.getCreatedAt(), notification.getUpdatedAt(),
            notification.getConfirmed(), notification.getContent(), notification.getUser().getId(),
            notification.getResourceType(), notification.getResourceId());
  }
}
