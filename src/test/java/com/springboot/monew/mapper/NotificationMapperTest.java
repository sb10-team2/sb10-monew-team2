package com.springboot.monew.mapper;

import com.springboot.monew.fixture.NotificationsFixture;
import com.springboot.monew.notification.dto.NotificationDto;
import com.springboot.monew.notification.entity.Notification;
import com.springboot.monew.notification.mapper.NotificationMapper;
import com.springboot.monew.notification.mapper.NotificationMapperImpl;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class NotificationMapperTest {
    private final NotificationMapper mapper = new NotificationMapperImpl();

    @Test
    @DisplayName("Notification 객체와 dto 공통 필드의 값이 같다")
    void entity_equal_to_dto() {
        // given
        Notification notification = NotificationsFixture.createEntityWithCommentLike();

        // when
        NotificationDto dto = mapper.toDto(notification);

        // then
        Assertions.assertThat(dto)
                .extracting("id", "createdAt", "updatedAt", "confirmed", "content", "userId", "resourceType", "resourceId")
                .contains(notification.getId(), notification.getCreatedAt(), notification.getUpdatedAt(), notification.getConfirmed(), notification.getContent(), notification.getUser().getId(), notification.getResourceType(), notification.getResourceId());
    }
}
