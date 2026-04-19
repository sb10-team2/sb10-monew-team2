package com.springboot.monew.notification.entity;

import static org.instancio.Select.field;

import com.springboot.monew.comment.entity.CommentLike;
import com.springboot.monew.common.fixture.EntityFixtureFactory;
import com.springboot.monew.interest.entity.Interest;
import com.springboot.monew.notification.exception.NotificationException;
import java.time.Instant;
import java.util.UUID;
import org.assertj.core.api.Assertions;
import org.instancio.Instancio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NotificationTest {

  @Test
  @DisplayName("interest 객체가 존재하면 interestId를 반환한다")
  void returnInterestId() {
    Notification notification = Instancio.of(Notification.class)
        .ignore(field(Notification::getCommentLike))
        .set(field(Notification::getResourceType), ResourceType.INTEREST)
        .create();
    UUID resourceId = notification.getInterest().map(Interest::getId).orElseThrow();
    Assertions.assertThat(resourceId).isEqualTo(notification.getResourceId());
  }

  @Test
  @DisplayName("commentLikes 객체가 존재하면 commentLikesId를 반환한다")
  void returnCommentLikesId() {
    Notification notification = Instancio.of(Notification.class)
        .ignore(field(Notification::getInterest))
        .set(field(Notification::getResourceType), ResourceType.COMMENT)
        .create();
    UUID resourceId = notification.getCommentLike().map(CommentLike::getId).orElseThrow();
    Assertions.assertThat(resourceId).isEqualTo(notification.getResourceId());
  }

  @Test
  @DisplayName("알람 확인 업데이트 성공")
  void successToUpdateConfirmed() {
    // given
    Instant updatedAt = Instant.now();
    Notification notification = EntityFixtureFactory.get(Notification.class);

    // when
    notification.updateConfirmed(updatedAt);

    // then
    Assertions.assertThat(notification)
        .extracting("confirmed", "updatedAt")
        .contains(true, updatedAt);
  }

  @Test
  @DisplayName("이미 확인한 알람을 또 다시 확인 업데이트 할 수 없다")
  void failToUpdateConfirmed() {
    // given
    Instant updatedAt = Instant.now();
    Notification notification = Instancio.of(Notification.class)
        .set(field(Notification::getConfirmed), false)
        .ignore(field(Notification::getUpdatedAt))
        .create();
    notification.updateConfirmed(updatedAt);

    // when & then
    Assertions.assertThatThrownBy(() -> notification.updateConfirmed(updatedAt))
        .isInstanceOf(NotificationException.class)
        .hasMessageContaining("이미 확인한 알람입니다");
  }
}
