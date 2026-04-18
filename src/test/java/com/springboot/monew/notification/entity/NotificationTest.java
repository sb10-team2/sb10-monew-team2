package com.springboot.monew.notification.entity;

import static org.instancio.Select.field;

import com.springboot.monew.comment.entity.CommentLike;
import com.springboot.monew.interest.entity.Interest;
import java.util.UUID;
import org.assertj.core.api.Assertions;
import org.instancio.Instancio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
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
}
