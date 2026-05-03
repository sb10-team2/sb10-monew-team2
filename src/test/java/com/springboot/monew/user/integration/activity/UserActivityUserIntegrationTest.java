package com.springboot.monew.user.integration.activity;

import static org.assertj.core.api.Assertions.assertThat;

import com.springboot.monew.common.integration.BaseIntegrationsTest;
import com.springboot.monew.newsarticles.entity.NewsArticle;
import com.springboot.monew.newsarticles.enums.ArticleSource;
import com.springboot.monew.newsarticles.repository.ArticleViewRepository;
import com.springboot.monew.newsarticles.repository.NewsArticleRepository;
import com.springboot.monew.newsarticles.service.NewsArticleService;
import com.springboot.monew.user.document.UserActivityDocument;
import com.springboot.monew.user.dto.request.UserRegisterRequest;
import com.springboot.monew.user.dto.request.UserUpdateRequest;
import com.springboot.monew.user.dto.response.UserActivityDto;
import com.springboot.monew.user.dto.response.UserDto;
import com.springboot.monew.user.outbox.UserActivityOutbox;
import com.springboot.monew.user.outbox.enums.UserActivityAggregateType;
import com.springboot.monew.user.outbox.enums.UserActivityEventType;
import com.springboot.monew.user.outbox.enums.UserActivityOutboxStatus;
import com.springboot.monew.user.repository.UserActivityOutboxRepository;
import com.springboot.monew.user.repository.UserActivityRepository;
import com.springboot.monew.user.repository.UserRepository;
import com.springboot.monew.user.service.UserActivityService;
import com.springboot.monew.user.service.UserService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class UserActivityUserIntegrationTest extends BaseIntegrationsTest {

  @Autowired
  private UserService userService;

  @Autowired
  private UserActivityService userActivityService;

  @Autowired
  private NewsArticleService newsArticleService;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private UserActivityRepository userActivityRepository;

  @Autowired
  private UserActivityOutboxRepository userActivityOutboxRepository;

  @Autowired
  private NewsArticleRepository newsArticleRepository;

  @Autowired
  private ArticleViewRepository articleViewRepository;

  @BeforeEach
  void setUp() {
    // 테스트 간 데이터 격리를 위해 Mongo 활동 문서와 기사 조회 이력, Outbox, 사용자 데이터를 초기화한다.
    userActivityRepository.deleteAll();
    userActivityOutboxRepository.deleteAll();
    articleViewRepository.deleteAll();
    newsArticleRepository.deleteAll();
    userRepository.deleteAll();
  }

  @Test
  @DisplayName("회원가입 이후 사용자 활동 문서와 USER_REGISTERED Outbox 이벤트가 함께 생성된다")
  void register_createsUserActivityDocumentAndOutbox() {
    // given
    UserRegisterRequest request = new UserRegisterRequest(
        "activity@test.com",
        "activityUser",
        "password123!"
    );

    // when
    // 실제 회원가입 서비스 진입점을 호출해 사용자 저장, Outbox 저장, 활동 문서 생성을 한 흐름으로 검증한다.
    UserDto result = userService.register(request);

    // then
    assertThat(result.email()).isEqualTo(request.email());
    assertThat(result.nickname()).isEqualTo(request.nickname());
    assertThat(result.createdAt()).isNotNull();
    assertThat(userRepository.findById(result.id())).isPresent();

    // 회원가입 시 USER_REGISTERED Outbox 이벤트가 1건 저장되어야 한다.
    List<UserActivityOutbox> outboxes = userActivityOutboxRepository.findAll();
    assertThat(outboxes).hasSize(1);

    UserActivityOutbox outbox = outboxes.get(0);
    assertThat(outbox.getEventType()).isEqualTo(UserActivityEventType.USER_REGISTERED);
    assertThat(outbox.getAggregateType()).isEqualTo(UserActivityAggregateType.USER);
    assertThat(outbox.getAggregateId()).isEqualTo(result.id());
    assertThat(outbox.getStatus()).isEqualTo(UserActivityOutboxStatus.PENDING);
    assertThat(outbox.getPayload()).contains(request.email());
    assertThat(outbox.getPayload()).contains(request.nickname());

    // 회원가입 이후 Mongo 사용자 활동 문서도 함께 생성되어야 한다.
    UserActivityDocument activity = userActivityRepository.findById(result.id()).orElseThrow();
    assertThat(activity.getId()).isEqualTo(result.id());
    assertThat(activity.getEmail()).isEqualTo(request.email());
    assertThat(activity.getNickname()).isEqualTo(request.nickname());
    assertThat(activity.getCreatedAt().truncatedTo(ChronoUnit.MILLIS))
        .isEqualTo(result.createdAt().truncatedTo(ChronoUnit.MILLIS));
    assertThat(activity.getSubscriptions()).isEmpty();
    assertThat(activity.getComments()).isEmpty();
    assertThat(activity.getCommentLikes()).isEmpty();
    assertThat(activity.getArticleViews()).isEmpty();
  }

  @Test
  @DisplayName("닉네임 수정 이후 사용자 활동 문서 닉네임과 USER_NICKNAME_UPDATED Outbox 이벤트가 함께 갱신된다")
  void update_updatesUserActivityNicknameAndCreatesOutbox() {
    // given
    UserDto registered = userService.register(new UserRegisterRequest(
        "update@test.com",
        "beforeNickname",
        "password123!"
    ));
    UserUpdateRequest request = new UserUpdateRequest("afterNickname");

    // 회원가입 시 생성된 Outbox 이벤트는 닉네임 수정 검증 대상에서 제외한다.
    userActivityOutboxRepository.deleteAll();

    // when
    // 실제 닉네임 수정 서비스 진입점을 호출해 사용자, Outbox, 활동 문서 갱신을 한 흐름으로 검증한다.
    UserDto updated = userService.update(registered.id(), request);

    // then
    assertThat(updated.id()).isEqualTo(registered.id());
    assertThat(updated.nickname()).isEqualTo(request.nickname());
    assertThat(userRepository.findById(registered.id())).isPresent()
        .get()
        .extracting(user -> user.getNickname())
        .isEqualTo(request.nickname());

    // 닉네임 수정 시 USER_NICKNAME_UPDATED Outbox 이벤트가 1건 저장되어야 한다.
    List<UserActivityOutbox> outboxes = userActivityOutboxRepository.findAll();
    assertThat(outboxes).hasSize(1);

    UserActivityOutbox outbox = outboxes.get(0);
    assertThat(outbox.getEventType()).isEqualTo(UserActivityEventType.USER_NICKNAME_UPDATED);
    assertThat(outbox.getAggregateType()).isEqualTo(UserActivityAggregateType.USER);
    assertThat(outbox.getAggregateId()).isEqualTo(registered.id());
    assertThat(outbox.getStatus()).isEqualTo(UserActivityOutboxStatus.PENDING);
    assertThat(outbox.getPayload()).contains(request.nickname());

    // Mongo 사용자 활동 문서의 닉네임도 최신 값으로 함께 갱신되어야 한다.
    UserActivityDocument activity = userActivityRepository.findById(registered.id()).orElseThrow();
    assertThat(activity.getNickname()).isEqualTo(request.nickname());
    assertThat(activity.getEmail()).isEqualTo(registered.email());
  }

  @Test
  @DisplayName("여러 사용자 활동이 반영된 뒤 조회 서비스를 호출하면 최근 기사 조회 목록은 최신순으로 최대 10개만 반환된다")
  void findUserActivity_returnsRecentArticleViewsInDescendingOrderAndLimit() {
    // given
    UserDto user = userService.register(new UserRegisterRequest(
        "activity-query@test.com",
        "activityQueryUser",
        "password123!"
    ));

    // 회원가입 시 생성된 USER_REGISTERED Outbox는 최종 조회 검증 대상에서 제외한다.
    userActivityOutboxRepository.deleteAll();

    List<NewsArticle> articles = new ArrayList<>();
    for (int i = 0; i < 11; i++) {
      articles.add(newsArticleRepository.save(NewsArticle.builder()
          .source(ArticleSource.NAVER)
          .originalLink("https://example.com/user-activity-query/" + i)
          .title("조회 기사 " + i)
          .publishedAt(Instant.parse("2026-05-01T00:00:00Z").plusSeconds(i))
          .summary("조회 기사 요약 " + i)
          .build()));
    }

    // 최근 활동 최대 개수와 최신순 정렬 정책을 검증하기 위해 기사 11개를 순서대로 조회한다.
    for (NewsArticle article : articles) {
      newsArticleService.createView(article.getId(), user.id());
    }

    // when
    // 여러 활동이 누적된 뒤 실제 사용자 활동 조회 서비스를 호출해 최종 응답 구조와 최근 활동 정책을 검증한다.
    UserActivityDto result = userActivityService.findUserActivity(user.id());

    // then
    assertThat(result.id()).isEqualTo(user.id());
    assertThat(result.email()).isEqualTo(user.email());
    assertThat(result.nickname()).isEqualTo(user.nickname());
    assertThat(result.subscriptions()).isEmpty();
    assertThat(result.comments()).isEmpty();
    assertThat(result.commentLikes()).isEmpty();

    // 기사 조회 목록은 최신순으로 최대 10개만 유지되어야 한다.
    assertThat(result.articleViews()).hasSize(10);
    assertThat(result.articleViews().get(0).articleId()).isEqualTo(articles.get(10).getId());
    assertThat(result.articleViews().get(0).articleTitle()).isEqualTo("조회 기사 10");
    assertThat(result.articleViews().get(9).articleId()).isEqualTo(articles.get(1).getId());
    assertThat(result.articleViews().get(9).articleTitle()).isEqualTo("조회 기사 1");

    // 가장 먼저 조회한 기사는 최대 개수 제한으로 응답에서 제외되어야 한다.
    assertThat(result.articleViews())
        .extracting(articleView -> articleView.articleId())
        .doesNotContain(articles.get(0).getId());
  }
}
