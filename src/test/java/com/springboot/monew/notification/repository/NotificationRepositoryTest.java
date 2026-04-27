package com.springboot.monew.notification.repository;

import com.springboot.monew.comment.entity.CommentLike;
import com.springboot.monew.common.repository.BaseRepositoryTest;
import com.springboot.monew.interest.entity.Interest;
import com.springboot.monew.notification.entity.Notification;
import com.springboot.monew.notification.entity.ResourceType;
import com.springboot.monew.users.entity.User;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import org.assertj.core.api.Assertions;
import org.instancio.Instancio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.test.util.ReflectionTestUtils;

public class NotificationRepositoryTest extends BaseRepositoryTest {

  @Autowired
  private NotificationRepository repository;

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
    repository.saveAndFlush(expected);
    printQueries();
    ensureQueryCount(1);
    clear();

    // then
    Notification actual = repository.findById(expected.getId()).orElseThrow();
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
    Assertions.assertThatThrownBy(() -> repository.saveAndFlush(expected))
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
        .interest(Instancio.create(Interest.class))
        .resourceType(ResourceType.INTEREST)
        .build();

    // when & then
    Assertions.assertThatThrownBy(() -> repository.saveAndFlush(expected))
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
    repository.saveAndFlush(expected);
    printQueries();
    ensureQueryCount(1);
    clear();

    // then
    Notification actual = repository.findById(expected.getId()).orElseThrow();
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
        .commentLike(Instancio.create(CommentLike.class))
        .resourceType(ResourceType.COMMENT)
        .build();

    // when & then
    Assertions.assertThatThrownBy(() -> repository.saveAndFlush(expected))
        .isInstanceOf(DataIntegrityViolationException.class)
        .rootCause()
        .message()
        .containsIgnoringCase("FK_NOTIFICATIONS_COMMENT_LIKES_ID");
  }

  @Test
  @DisplayName("""
      comment_like_id가 존재하면 resource_type은 'COMMENT'이어야 한다
      domain integrity violation
      객체 생성 단계에서 위 과정을 검사한다
      그래서 객체를 만들고 reflection으로 값을 주입 하겠다""")
  void failToCreateDueToMismatchResourceType() {
    // given
    Notification expected = Notification.builder()
        .user(testEntityManager.generateUser())
        .commentLike(testEntityManager.generateCommentLike())
        .resourceType(ResourceType.COMMENT)
        .build();
    ReflectionTestUtils.setField(expected, "resourceType", ResourceType.INTEREST);

    // when & then
    Assertions.assertThatThrownBy(() -> repository.saveAndFlush(expected))
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
    Slice<Notification> result = repository.findByCursor(null, null, user.getId(),
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
    Instant after = null;
    List<UUID> allFetchedIds = new ArrayList<>();
    boolean hasNext = true;

    Pageable pageable = PageRequest.of(0, pageSize);

    // when & then
    while (hasNext) {
      Slice<Notification> result = repository.findByCursor(cursor, after, userA.getId(),
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
        after = actual.get(actual.size() - 1).getCreatedAt();
      }
    }

    // verify
    Assertions.assertThat(allFetchedIds)
        .hasSize(totalSize)
        .containsExactlyInAnyOrderElementsOf(notificationIdsForA);
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
    UUID userId = expected.get(0).getUser().getId();
    int updatedSuccessCount = repository.bulkUpdateConfirmed(userId, updatedAt);
    ensureQueryCount(1);
    printQueries();

    // then
    Assertions.assertThat(updatedSuccessCount).isEqualTo(num);
    List<Notification> actual = repository.findAll();
    Assertions.assertThat(actual)
        .usingRecursiveComparison()
        .ignoringFields("user", "interest")
        .ignoringCollectionOrder()
        .withEqualsForType(this::compareInstant, Instant.class)
        .isEqualTo(expected);
  }

  @Test
  @DisplayName("""
      조건에 맞는 데이터들 삭제 성공
      확인한 날짜를 임의로 1~2주 전으로 저장한다
      확인한 날짜가 1주일 지났으면 삭제 대상이다""")
  void successToDelete() {
    // given
    int size = 100;
    Instant now = Instant.now();
    Instant threshold = now.minus(7, ChronoUnit.DAYS);
    Instant twoWeekAgo = threshold.minus(7, ChronoUnit.DAYS);
    Supplier<Instant> past = () -> Instancio.gen().temporal().instant().range(twoWeekAgo, now)
        .get();
    List<Notification> entities = testEntityManager.generateNotifications(size);
    entities.forEach(n -> n.updateConfirmed(past.get()));
    long expected = entities.stream().filter(n -> threshold.isAfter(n.getUpdatedAt())).count();
    em.flush();
    clear();

    // when
    long actual = repository.deleteOutdatedByChunk(threshold, size);
    ensureQueryCount(1);
    printQueries();

    // then
    Assertions.assertThat(actual).isEqualTo(expected);
    List<Notification> results = repository.findAll();
    Assertions.assertThat(results.size()).isEqualTo(size - expected);
  }

  @Test
  @DisplayName("cursor=null, after=null, limit=50, userId=... 에 대해 알림 데이터가 없어도 조회 성공")
  void successToFindByCursorWithNoData() {
    // given
    UUID userId = UUID.randomUUID();
    UUID cursor = null;
    Instant after = null;
    int limit = 50;

    // when
    Slice<Notification> actual = repository.findByCursor(cursor, after, userId, PageRequest.of(0, limit));

    // then
    Assertions.assertThat(actual.getContent()).hasSize(0);
  }
}
