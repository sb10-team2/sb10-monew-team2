package com.springboot.monew.notification.mapper;

import com.springboot.monew.common.fixture.NotificationFixture;
import com.springboot.monew.notification.dto.NotificationDto;
import com.springboot.monew.notification.entity.Notification;
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
    void entityIsEqualToDto() {
        // given
        Notification notification = NotificationFixture.createEntityWithCommentLike();

        // when
        NotificationDto dto = mapper.toDto(notification);

        // then
        Assertions.assertThat(dto)
                .extracting("id", "createdAt", "updatedAt", "confirmed", "content", "userId", "resourceType", "resourceId")
                .contains(notification.getId(), notification.getCreatedAt(), notification.getUpdatedAt(), notification.getConfirmed(), notification.getContent(), notification.getUser().getId(), notification.getResourceType(), notification.getResourceId());
    }
}
