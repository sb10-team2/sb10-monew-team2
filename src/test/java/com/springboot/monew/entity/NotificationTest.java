package com.springboot.monew.entity;

import com.springboot.monew.fixture.NotificationsFixture;
import com.springboot.monew.notification.entity.Notification;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class NotificationTest {

    @Test
    @DisplayName("interest 객체가 존재하면 interestId를 반환한다")
    void return_interest_id() {
        Notification notification = NotificationsFixture.createEntityWithInterest();
        UUID resourceId = notification.getInterest().map(Interest::getId).orElseThrow();
        Assertions.assertThat(resourceId).isEqualTo(notification.getResourceId());
    }

    @Test
    @DisplayName("commentLikes 객체가 존재하면 commentLikesId를 반환한다")
    void return_commentLikes_id() {
        Notification notification = NotificationsFixture.createEntityWithCommentLike();
        UUID resourceId = notification.getCommentLike().map(CommentLike::getId).orElseThrow();
        Assertions.assertThat(resourceId).isEqualTo(notification.getResourceId());
    }
}
