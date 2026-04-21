package com.springboot.monew.notification.repository;

import com.springboot.monew.common.repository.BaseRepositoryTest;
import com.springboot.monew.common.utils.TimeConverter;
import com.springboot.monew.notification.entity.Notification;
import com.springboot.monew.notification.entity.ResourceType;
import com.springboot.monew.users.entity.User;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

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

  @Test
  @DisplayName("""
      cursor pagination 조회 pageSize=10, cursor=null, after=null, confirmed=false
      12개의 알람 객체를 저장
      알람 객체가 참조하는 유저는 모두 동일하도록 테스트 객체를 생성하였다""")
  void successToFindByCursor() {
    // given
    int totalSize = 12;
    int pageSize = 10;
    List<Notification> expected = testEntityManager.generateNotifications(totalSize);
    User user = expected.get(0).getUser();
    printQueries();
    ensureQueryCount(3);
    clear();

    // when
    Pageable pageable = PageRequest.of(0, pageSize);
    Slice<Notification> result = notificationRepository.findByCursor(null, null, user.getId(),
        pageable);
    List<Notification> actual = result.getContent();
    printQueries();
    ensureQueryCount(1);

    // then
    Assertions.assertThat(result.hasNext()).isTrue();
    Assertions.assertThat(actual)
        .hasSize(pageSize)
        .doesNotHaveDuplicates()
        .extracting(Notification::getId)
        .isSubsetOf(expected.stream().map(Notification::getId).toList());
  }

  @Test
  @DisplayName("""
      유저A, B 각각 확인하지 않은 알람 10개가 있다
      유저A 알람을 6개씩 2번 조회할 때 유저B의 알람을 가져오지 않는다
      cursor pagination 격리 및 완전성 테스트
      """)
  void isolationTestForCursorPagination() {
    // given
    int totalSize = 10;
    int pageSize = 6;

    List<Notification> expectedA = testEntityManager.generateNotifications(totalSize);
    User userA = expectedA.get(0).getUser();
    List<UUID> notificationIdsForA = getIds(expectedA);

    List<Notification> expectedB = testEntityManager.generateNotifications(totalSize);
//    User userB = expectedB.get(0).getUser(); // note that userA.getId() != userB.getId()
    List<UUID> notificationIdsForB = getIds(expectedB);

    clear();

    UUID cursor = null;
    LocalDateTime after = null;
    List<UUID> allFetchedIds = new ArrayList<>();
    boolean hasNext = true;

    Pageable pageable = PageRequest.of(0, pageSize);

    // when & then
    while (hasNext) {
      Slice<Notification> result = notificationRepository.findByCursor(cursor, after, userA.getId(),
          pageable);
      List<Notification> actual = result.getContent();

      Assertions.assertThat(actual)
          .doesNotHaveDuplicates()
          .extracting(Notification::getId)
          .isSubsetOf(notificationIdsForA)
          .doesNotContainAnyElementsOf(notificationIdsForB);

      allFetchedIds.addAll(getIds(actual));

      hasNext = result.hasNext();
      if (hasNext && !actual.isEmpty()) {
        cursor = actual.get(actual.size() - 1).getId();
        after = TimeConverter.toDatetime(actual.get(actual.size() - 1).getCreatedAt());
      }
    }

    // verify
    Assertions.assertThat(allFetchedIds)
        .hasSize(totalSize)
        .containsExactlyInAnyOrderElementsOf(notificationIdsForA);
  }

  @Test
  @DisplayName("알람 객체 업데이트 성공\n"
      + "confirmed=false -> true, updatedAt=Instant.now()")
  void successToUpdateConfirmed() {
    // given
    Instant updatedAt = Instant.now();
    Notification expected = testEntityManager.generateNotification();
    expected.updateConfirmed(updatedAt);
    clear();

    // when
    notificationRepository.updateConfirmed(expected.getId(), expected.getUser().getId(),
        updatedAt);
    ensureQueryCount(1);
    printQueries();

    // then
    Notification actual = notificationRepository.findById(expected.getId()).orElseThrow();
    Assertions.assertThat(actual)
        .usingRecursiveComparison()
        .ignoringFields("user", "interest")
        .withEqualsForType(this::compareInstant, Instant.class)
        .isEqualTo(expected);
  }

  @Test
  @DisplayName("알람 벌크 업데이트 성공\n"
      + "confirmed=false -> true, updatedAt=Instant.now()")
  void successToBulkUpdateConfirmed() {
    // given
    int num = 10;
    Instant updatedAt = Instant.now();
    List<Notification> expected = testEntityManager.generateNotifications(num);
    expected.forEach(notification -> notification.updateConfirmed(updatedAt));
    clear();

    // when
    List<UUID> ids = getIds(expected);
    UUID userId = expected.get(0).getUser().getId();
    int updatedSuccessCount = notificationRepository.updateConfirmed(ids, userId, updatedAt);
    ensureQueryCount(1);
    printQueries();

    // then
    Assertions.assertThat(updatedSuccessCount).isEqualTo(num);
    List<Notification> actual = notificationRepository.findAll();
    Assertions.assertThat(actual)
        .usingRecursiveComparison()
        .ignoringFields("user", "interest")
        .ignoringCollectionOrder()
        .withEqualsForType(this::compareInstant, Instant.class)
        .isEqualTo(expected);
  }
}
