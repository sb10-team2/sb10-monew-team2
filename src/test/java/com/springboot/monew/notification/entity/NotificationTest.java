package com.springboot.monew.notification.entity;

import static org.instancio.Select.field;

import com.springboot.monew.comment.entity.CommentLike;
import com.springboot.monew.common.fixture.EntityFixtureFactory;
import com.springboot.monew.interest.entity.Interest;
import com.springboot.monew.notification.exception.NotificationException;
import com.springboot.monew.users.entity.User;
import java.time.Instant;
import java.util.Optional;
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
    Optional<Interest> interest = Optional.ofNullable(notification.getInterest());
    UUID resourceId = interest.map(Interest::getId).orElseThrow();
    Assertions.assertThat(resourceId).isEqualTo(notification.getResourceId());
  }

  @Test
  @DisplayName("commentLikes 객체가 존재하면 commentLikesId를 반환한다")
  void returnCommentLikesId() {
    Notification notification = Instancio.of(Notification.class)
        .ignore(field(Notification::getInterest))
        .set(field(Notification::getResourceType), ResourceType.COMMENT)
        .create();
    Optional<CommentLike> commentLike = Optional.ofNullable(notification.getCommentLike());
    UUID resourceId = commentLike.map(CommentLike::getId).orElseThrow();
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
        .hasMessageContaining("이미 읽음 처리된 알림입니다");
  }

  @Test
  @DisplayName("""
      Interest를 주입받아 객체 생성 성공
      도메인 제약 조건 통과
      content는 '{관심사 이름}와 관련된 기사가 {관심사와 관련된 기사 갯수}건 등록되었습니다.' 포맷을 가져야 한다""")
  void successToInitWithInterest() {
    // given
    Interest interest = EntityFixtureFactory.get(Interest.class);
    User user = EntityFixtureFactory.get(User.class);

    // when
    Notification entity = Notification.builder()
        .resourceType(ResourceType.INTEREST)
        .user(user)
        .interest(interest)
        .build();

    // then
    String interestMessage = "%s와 관련된 기사가 %s건 등록되었습니다.";
    Assertions.assertThat(entity)
        .extracting("content")
        .isEqualTo(interestMessage.formatted(interest.getName(), interest.getArticleCount()));
  }

  @Test
  @DisplayName("""
      CommentLike를 주입받아 객체 생성 성공
      도메인 제약 조건 통과
      content는 '{좋아요를 누른 유저 닉네임}님이 나의 댓글을 좋아합니다.' 포맷을 가져야 한다""")
  void successToInitWithCommentLike() {
    // given
    CommentLike commentLike = EntityFixtureFactory.get(CommentLike.class);
    User user = EntityFixtureFactory.get(User.class);

    // when
    Notification entity = Notification.builder()
        .resourceType(ResourceType.COMMENT)
        .user(user)
        .commentLike(commentLike)
        .build();

    // then
    String interestMessage = "%s님이 나의 댓글을 좋아합니다.";
    Assertions.assertThat(entity)
        .extracting("content")
        .isEqualTo(interestMessage.formatted(commentLike.getUser().getNickname()));
  }

  @Test
  @DisplayName("Interest를 주입받았지만 ResourceType=COMMENT로 인해 객체 생성 실패")
  void failToInitWithInterestDueToInconsistentResourceType() {
    // given
    Interest interest = EntityFixtureFactory.get(Interest.class);
    User user = EntityFixtureFactory.get(User.class);

    // when & then
    Assertions.assertThatThrownBy(() ->
            Notification.builder()
                .user(user)
                .interest(interest)
                .resourceType(ResourceType.COMMENT)
                .build())
        .isInstanceOf(NotificationException.class);
  }

  @Test
  @DisplayName("CommentLike를 주입받았지만 ResourceType=INTEREST로 인해 객체 생성 실패")
  void failToInitWithCommentLikeDueToInconsistentResourceType() {
    // given
    CommentLike commentLike = EntityFixtureFactory.get(CommentLike.class);
    User user = EntityFixtureFactory.get(User.class);

    // when & then
    Assertions.assertThatThrownBy(() ->
            Notification.builder()
                .user(user)
                .commentLike(commentLike)
                .resourceType(ResourceType.INTEREST)
                .build())
        .isInstanceOf(NotificationException.class);
  }

  @Test
  @DisplayName("User=null 이면 객체 생성 실패")
  void failToInitWithoutUser() {
    // given
    CommentLike commentLike = EntityFixtureFactory.get(CommentLike.class);
    User user = null;

    // when & then
    Assertions.assertThatThrownBy(() ->
            Notification.builder()
                .user(user)
                .commentLike(commentLike)
                .resourceType(ResourceType.COMMENT)
                .build())
        .isInstanceOf(NotificationException.class);
  }

  @Test
  @DisplayName("Interest, CommentLike 둘 다 null이 아닌 객체를 입력 받으면 객체 생성 실패")
  void failToInitWithInterestAndCommentLike() {
    // given
    User user = EntityFixtureFactory.get(User.class);
    CommentLike commentLike = EntityFixtureFactory.get(CommentLike.class);
    Interest interest = EntityFixtureFactory.get(Interest.class);

    // when & then
    Assertions.assertThatThrownBy(() ->
            Notification.builder()
                .user(user)
                .commentLike(commentLike)
                .interest(interest)
                .resourceType(ResourceType.INTEREST)
                .build())
        .isInstanceOf(NotificationException.class);
  }
}
