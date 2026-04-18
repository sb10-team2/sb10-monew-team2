package com.springboot.monew.notification.repository;

import com.springboot.monew.common.repository.BaseRepositoryTest;
import com.springboot.monew.notification.entity.Notification;
import com.springboot.monew.notification.entity.ResourceType;
import java.time.Instant;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

public class NotificationRepositoryTest extends BaseRepositoryTest {

  @Autowired
  private NotificationRepository notificationRepository;

  @BeforeEach
  void setUp() {
    queryInspector.clear();
  }

  @Test
  @DisplayName("""
      관심사와 함께 알람 객체가 저장되고 조회한다
      생성 쿼리 1번""")
  void successToCreateByInterestAndFind() {
    // given
    Notification expected = Notification.builder()
        .content("successToCreateByInterestAndFind")
        .resourceType(ResourceType.INTEREST)
        .user(testEntityManager.generateUser())
        .interest(testEntityManager.generateInterest())
        .build();
    clear();

    // when
    notificationRepository.saveAndFlush(expected);
    printQueries();
    ensureQueryCount(1);
    clear();

    // then
    Notification actual = notificationRepository.findById(expected.getId()).orElseThrow();
    Assertions.assertThat(actual)
        .usingRecursiveComparison()
        .ignoringFields("user", "commentLike", "interest")
        .withEqualsForType(this::compareInstant, Instant.class)
        .isEqualTo(expected);
    printQueries();
    ensureQueryCount(1);
  }

  @Test
  @DisplayName("존재하지 않는 유저에 대해 알람 생성이 실패한다\n"
      + "user 외래 키 참조 무결성 위반")
  void failToCreateWithNonExistingUser() {
    // given
    Notification expected = Notification.builder()
        .content("failToCreateWithNonExistingUser")
        .user(testEntityManager.getProxyUser())
        .interest(testEntityManager.generateInterest())
        .resourceType(ResourceType.INTEREST)
        .build();

    // when & then
    Assertions.assertThatThrownBy(() -> notificationRepository.saveAndFlush(expected))
        .isInstanceOf(DataIntegrityViolationException.class)
        .rootCause()
        .message()
        .containsIgnoringCase("FK_NOTIFICATIONS_USER_ID");
  }

  @Test
  @DisplayName("존재하지 않는 관심사에 대해 알람 생성이 실패한다\n"
      + "interest 외래 키 참조 무결성 위반")
  void failToCreateWithNonExistingInterest() {
    // given
    Notification expected = Notification.builder()
        .content("failToCreateWithNonExistingInterest")
        .user(testEntityManager.generateUser())
        .interest(testEntityManager.getProxyInterest())
        .resourceType(ResourceType.INTEREST)
        .build();

    // when & then
    Assertions.assertThatThrownBy(() -> notificationRepository.saveAndFlush(expected))
        .isInstanceOf(DataIntegrityViolationException.class)
        .rootCause()
        .message()
        .containsIgnoringCase("FK_NOTIFICATIONS_INTEREST_ID");
  }

  @Test
  @DisplayName("댓글 좋아요 생성 시 알람 생성하고 조회한다\n" +
      "생성 쿼리 1번")
  void successToCreateWithCommentLikeAndFind() {
    // given
    Notification expected = Notification.builder()
        .content("successToCreateWithCommentLikeAndFind")
        .user(testEntityManager.generateUser())
        .commentLike(testEntityManager.generateCommentLike())
        .resourceType(ResourceType.COMMENT)
        .build();
    clear();

    // when
    notificationRepository.saveAndFlush(expected);
    printQueries();
    ensureQueryCount(1);
    clear();

    // then
    Notification actual = notificationRepository.findById(expected.getId()).orElseThrow();
    Assertions.assertThat(actual)
        .usingRecursiveComparison()
        .ignoringFields("user", "commentLike", "interest")
        .withEqualsForType(this::compareInstant, Instant.class)
        .isEqualTo(expected);
    printQueries();
    ensureQueryCount(1);
  }

  @Test
  @DisplayName("존재하지 않는 댓글 좋아요에 대해 알람 생성이 실패한다\n"
      + "commentLike 외래 키 참조 무결성 위반")
  void failToCreateWithNonExistingCommentLike() {
    // given
    Notification expected = Notification.builder()
        .content("failToCreateWithNonExistingCommentLike")
        .user(testEntityManager.generateUser())
        .commentLike(testEntityManager.getProxyCommentLike())
        .resourceType(ResourceType.COMMENT)
        .build();

    // when & then
    Assertions.assertThatThrownBy(() -> notificationRepository.saveAndFlush(expected))
        .isInstanceOf(DataIntegrityViolationException.class)
        .rootCause()
        .message()
        .containsIgnoringCase("FK_NOTIFICATIONS_COMMENT_LIKES_ID");
  }

  @Test
  @DisplayName("comment_like_id가 존재하면 resource_type은 'COMMENT'이어야 한다\n"
      + "domain integrity violation")
  void failToCreateDueToMismatchResourceType() {
    // given
    Notification expected = Notification.builder()
        .content("failToCreateDueToMismatchResourceType")
        .user(testEntityManager.generateUser())
        .commentLike(testEntityManager.generateCommentLike())
        .resourceType(ResourceType.INTEREST)
        .build();

    // when & then
    Assertions.assertThatThrownBy(() -> notificationRepository.saveAndFlush(expected))
        .isInstanceOf(DataIntegrityViolationException.class)
        .rootCause()
        .message()
        .containsIgnoringCase("chk_notification_polymorphic_match");
  }
}
