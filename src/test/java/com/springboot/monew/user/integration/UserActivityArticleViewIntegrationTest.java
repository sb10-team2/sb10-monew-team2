package com.springboot.monew.user.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.springboot.monew.common.integration.BaseIntegrationsTest;
import com.springboot.monew.newsarticles.dto.response.NewsArticleViewDto;
import com.springboot.monew.newsarticles.entity.NewsArticle;
import com.springboot.monew.newsarticles.enums.ArticleSource;
import com.springboot.monew.newsarticles.repository.ArticleViewRepository;
import com.springboot.monew.newsarticles.repository.NewsArticleRepository;
import com.springboot.monew.newsarticles.service.NewsArticleService;
import com.springboot.monew.user.document.UserActivityDocument;
import com.springboot.monew.user.dto.request.UserRegisterRequest;
import com.springboot.monew.user.dto.response.UserDto;
import com.springboot.monew.user.outbox.UserActivityOutbox;
import com.springboot.monew.user.outbox.enums.UserActivityAggregateType;
import com.springboot.monew.user.outbox.enums.UserActivityEventType;
import com.springboot.monew.user.outbox.enums.UserActivityOutboxStatus;
import com.springboot.monew.user.repository.UserActivityOutboxRepository;
import com.springboot.monew.user.repository.UserActivityRepository;
import com.springboot.monew.user.repository.UserRepository;
import com.springboot.monew.user.service.UserService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class UserActivityArticleViewIntegrationTest extends BaseIntegrationsTest {

  @Autowired
  private UserService userService;

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

  private UserDto viewer;
  private NewsArticle article;

  @BeforeEach
  void setUp() {
    // 테스트 간 데이터 격리를 위해 Mongo 활동 문서와 기사 조회 관련 RDB 데이터를 초기화한다.
    userActivityRepository.deleteAll();
    userActivityOutboxRepository.deleteAll();
    articleViewRepository.deleteAll();
    newsArticleRepository.deleteAll();
    userRepository.deleteAll();

    // 기사 조회 사용자를 실제 회원가입 서비스로 생성해 사용자 활동 문서를 함께 준비한다.
    viewer = userService.register(new UserRegisterRequest(
        "viewer@test.com",
        "viewerUser",
        "password123!"
    ));

    // 회원가입 시 생성된 USER_REGISTERED Outbox 이벤트는 기사 조회 흐름 검증 대상에서 제외하기 위해 비운다.
    userActivityOutboxRepository.deleteAll();

    article = newsArticleRepository.save(NewsArticle.builder()
        .source(ArticleSource.NAVER)
        .originalLink("https://example.com/article/user-activity-view")
        .title("기사 조회 활동 테스트 기사")
        .publishedAt(Instant.parse("2026-05-01T00:00:00Z"))
        .summary("기사 조회 활동 테스트 요약")
        .build());
  }

  @Test
  @DisplayName("기사 조회 이후 활동 문서에 기사 조회 내역과 ARTICLE_VIEWED Outbox 이벤트가 추가된다")
  void createView_addsArticleViewToUserActivityAndCreatesOutbox() {
    // when
    // 실제 기사 조회 서비스 진입점을 호출해 article_view 저장, Outbox 저장, 활동 문서 반영을 한 흐름으로 검증한다.
    NewsArticleViewDto viewed = newsArticleService.createView(article.getId(), viewer.id());

    // then
    assertThat(viewed.articleId()).isEqualTo(article.getId());
    assertThat(viewed.viewedBy()).isEqualTo(viewer.id());
    assertThat(viewed.articleTitle()).isEqualTo(article.getTitle());
    assertThat(viewed.articleViewCount()).isEqualTo(1L);

    // 기사 조회 row가 실제로 저장되어야 한다.
    assertThat(articleViewRepository.existsByNewsArticleIdAndUserId(article.getId(), viewer.id()))
        .isTrue();

    // 기사 조회 후 ARTICLE_VIEWED Outbox 이벤트가 1건 저장되어야 한다.
    List<UserActivityOutbox> outboxes = userActivityOutboxRepository.findAll();
    assertThat(outboxes).hasSize(1);

    UserActivityOutbox outbox = outboxes.get(0);
    assertThat(outbox.getEventType()).isEqualTo(UserActivityEventType.ARTICLE_VIEWED);
    assertThat(outbox.getAggregateType()).isEqualTo(UserActivityAggregateType.ARTICLE_VIEW);
    assertThat(outbox.getAggregateId()).isEqualTo(viewed.id());
    assertThat(outbox.getStatus()).isEqualTo(UserActivityOutboxStatus.PENDING);
    assertThat(outbox.getPayload()).contains(article.getTitle());
    assertThat(outbox.getPayload()).contains(article.getSummary());

    // Mongo 사용자 활동 문서에 기사 조회 내역이 추가되어야 한다.
    UserActivityDocument activity = userActivityRepository.findById(viewer.id()).orElseThrow();
    assertThat(activity.getArticleViews()).hasSize(1);
    assertThat(activity.getArticleViews().get(0).id()).isEqualTo(viewed.id());
    assertThat(activity.getArticleViews().get(0).articleId()).isEqualTo(article.getId());
    assertThat(activity.getArticleViews().get(0).articleTitle()).isEqualTo(article.getTitle());
    assertThat(activity.getArticleViews().get(0).articleViewCount()).isEqualTo(1L);
  }
}
