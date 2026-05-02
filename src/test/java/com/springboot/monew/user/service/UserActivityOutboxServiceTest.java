package com.springboot.monew.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.springboot.monew.newsarticles.enums.ArticleSource;
import com.springboot.monew.user.document.UserActivityDocument.ArticleViewItem;
import com.springboot.monew.user.entity.User;
import com.springboot.monew.user.outbox.UserActivityOutbox;
import com.springboot.monew.user.outbox.UserActivityOutboxPayloadSerializer;
import com.springboot.monew.user.outbox.enums.UserActivityAggregateType;
import com.springboot.monew.user.outbox.enums.UserActivityEventType;
import com.springboot.monew.user.outbox.enums.UserActivityOutboxStatus;
import com.springboot.monew.user.outbox.payload.articleview.ArticleViewedPayload;
import com.springboot.monew.user.outbox.payload.comment.CommentDeletedPayload;
import com.springboot.monew.user.outbox.payload.commentlike.CommentLikeCountUpdatedPayload;
import com.springboot.monew.user.outbox.payload.interest.InterestSubscribedPayload;
import com.springboot.monew.user.outbox.payload.user.UserRegisteredPayload;
import com.springboot.monew.user.repository.UserActivityOutboxRepository;
import com.springboot.monew.interest.entity.Interest;
import com.springboot.monew.interest.entity.Subscription;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserActivityOutboxServiceTest {

  @Mock
  private UserActivityOutboxRepository userActivityOutboxRepository;

  @Mock
  private UserActivityOutboxPayloadSerializer payloadSerializer;

  @InjectMocks
  private UserActivityOutboxService userActivityOutboxService;

  @Test
  @DisplayName("payload를 JSON으로 직렬화해 UserActivityOutbox row로 저장한다")
  void save_serializesPayloadAndSavesOutbox() {
    // given
    UUID aggregateId = UUID.randomUUID();
    Object payload = new Object();
    String payloadJson = "{\"event\":\"payload\"}";

    // payload 직렬화 결과를 미리 준비한다.
    given(payloadSerializer.toJson(payload)).willReturn(payloadJson);

    // 저장소 save는 전달된 Outbox 엔티티를 그대로 반환하도록 설정한다.
    given(userActivityOutboxRepository.save(any(UserActivityOutbox.class)))
        .willAnswer(invocation -> invocation.getArgument(0));

    // when
    UserActivityOutbox saved = userActivityOutboxService.save(
        UserActivityEventType.USER_REGISTERED,
        UserActivityAggregateType.USER,
        aggregateId,
        payload
    );

    // then
    // payload가 먼저 JSON 문자열로 직렬화되어야 한다.
    verify(payloadSerializer).toJson(payload);

    // 저장소에 전달된 Outbox 엔티티를 캡처해 저장 값이 올바른지 검증한다.
    ArgumentCaptor<UserActivityOutbox> captor = ArgumentCaptor.forClass(UserActivityOutbox.class);
    verify(userActivityOutboxRepository).save(captor.capture());

    UserActivityOutbox outbox = captor.getValue();

    assertThat(outbox.getEventType()).isEqualTo(UserActivityEventType.USER_REGISTERED);
    assertThat(outbox.getAggregateType()).isEqualTo(UserActivityAggregateType.USER);
    assertThat(outbox.getAggregateId()).isEqualTo(aggregateId);
    assertThat(outbox.getPayload()).isEqualTo(payloadJson);
    assertThat(outbox.getStatus()).isEqualTo(UserActivityOutboxStatus.PENDING);
    assertThat(outbox.getRetryCount()).isZero();
    assertThat(outbox.getOccurredAt()).isNotNull();

    // save 메서드는 저장소가 반환한 Outbox 엔티티를 그대로 반환해야 한다.
    assertThat(saved).isSameAs(outbox);
  }

  @Test
  @DisplayName("회원가입 이벤트를 USER_REGISTERED Outbox로 저장한다")
  void saveUserRegistered_savesUserRegisteredOutbox() {
    // given
    UUID userId = UUID.randomUUID();
    Instant createdAt = Instant.parse("2026-05-01T00:00:00Z");
    String payloadJson = "{\"event\":\"user-registered\"}";

    User user = org.mockito.Mockito.mock(User.class);
    given(user.getId()).willReturn(userId);
    given(user.getEmail()).willReturn("test@example.com");
    given(user.getNickname()).willReturn("tester");
    given(user.getCreatedAt()).willReturn(createdAt);

    UserRegisteredPayload payload = UserRegisteredPayload.of(user);

    // 회원가입 payload 직렬화 결과를 미리 준비한다.
    given(payloadSerializer.toJson(payload)).willReturn(payloadJson);
    given(userActivityOutboxRepository.save(any(UserActivityOutbox.class)))
        .willAnswer(invocation -> invocation.getArgument(0));

    // when
    userActivityOutboxService.saveUserRegistered(user);

    // then
    // USER_REGISTERED 이벤트와 USER aggregate로 저장되었는지 검증한다.
    ArgumentCaptor<UserActivityOutbox> captor = ArgumentCaptor.forClass(UserActivityOutbox.class);
    verify(userActivityOutboxRepository).save(captor.capture());

    UserActivityOutbox outbox = captor.getValue();
    assertThat(outbox.getEventType()).isEqualTo(UserActivityEventType.USER_REGISTERED);
    assertThat(outbox.getAggregateType()).isEqualTo(UserActivityAggregateType.USER);
    assertThat(outbox.getAggregateId()).isEqualTo(userId);
    assertThat(outbox.getPayload()).isEqualTo(payloadJson);
  }

  @Test
  @DisplayName("관심사 구독 이벤트를 INTEREST_SUBSCRIBED Outbox로 저장한다")
  void saveInterestSubscribed_savesInterestSubscribedOutbox() {
    // given
    UUID subscriptionId = UUID.randomUUID();
    UUID interestId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    Instant createdAt = Instant.parse("2026-05-01T00:00:00Z");
    List<String> keywords = List.of("주식", "채권");
    String payloadJson = "{\"event\":\"interest-subscribed\"}";

    User user = org.mockito.Mockito.mock(User.class);
    Interest interest = org.mockito.Mockito.mock(Interest.class);
    Subscription subscription = org.mockito.Mockito.mock(Subscription.class);

    given(user.getId()).willReturn(userId);
    given(interest.getId()).willReturn(interestId);
    given(interest.getName()).willReturn("금융");
    given(subscription.getId()).willReturn(subscriptionId);
    given(subscription.getUser()).willReturn(user);
    given(subscription.getInterest()).willReturn(interest);
    given(subscription.getCreatedAt()).willReturn(createdAt);

    InterestSubscribedPayload payload = InterestSubscribedPayload.of(subscription, keywords);

    // 관심사 구독 payload 직렬화 결과를 미리 준비한다.
    given(payloadSerializer.toJson(payload)).willReturn(payloadJson);
    given(userActivityOutboxRepository.save(any(UserActivityOutbox.class)))
        .willAnswer(invocation -> invocation.getArgument(0));

    // when
    userActivityOutboxService.saveInterestSubscribed(subscription, keywords);

    // then
    // INTEREST_SUBSCRIBED 이벤트와 SUBSCRIPTION aggregate로 저장되었는지 검증한다.
    ArgumentCaptor<UserActivityOutbox> captor = ArgumentCaptor.forClass(UserActivityOutbox.class);
    verify(userActivityOutboxRepository).save(captor.capture());

    UserActivityOutbox outbox = captor.getValue();
    assertThat(outbox.getEventType()).isEqualTo(UserActivityEventType.INTEREST_SUBSCRIBED);
    assertThat(outbox.getAggregateType()).isEqualTo(UserActivityAggregateType.SUBSCRIPTION);
    assertThat(outbox.getAggregateId()).isEqualTo(subscriptionId);
    assertThat(outbox.getPayload()).isEqualTo(payloadJson);
  }

  @Test
  @DisplayName("댓글 좋아요 수 변경 이벤트를 COMMENT_LIKE_COUNT_UPDATED Outbox로 저장한다")
  void saveCommentLikeCountUpdated_savesCommentLikeCountUpdatedOutbox() {
    // given
    UUID userId = UUID.randomUUID();
    UUID commentId = UUID.randomUUID();
    long likeCount = 5L;
    String payloadJson = "{\"event\":\"comment-like-count-updated\"}";

    CommentLikeCountUpdatedPayload payload =
        CommentLikeCountUpdatedPayload.of(userId, commentId, likeCount);

    // 댓글 좋아요 수 변경 payload 직렬화 결과를 미리 준비한다.
    given(payloadSerializer.toJson(payload)).willReturn(payloadJson);
    given(userActivityOutboxRepository.save(any(UserActivityOutbox.class)))
        .willAnswer(invocation -> invocation.getArgument(0));

    // when
    userActivityOutboxService.saveCommentLikeCountUpdated(userId, commentId, likeCount);

    // then
    // COMMENT_LIKE_COUNT_UPDATED 이벤트와 COMMENT aggregate로 저장되었는지 검증한다.
    ArgumentCaptor<UserActivityOutbox> captor = ArgumentCaptor.forClass(UserActivityOutbox.class);
    verify(userActivityOutboxRepository).save(captor.capture());

    UserActivityOutbox outbox = captor.getValue();
    assertThat(outbox.getEventType()).isEqualTo(UserActivityEventType.COMMENT_LIKE_COUNT_UPDATED);
    assertThat(outbox.getAggregateType()).isEqualTo(UserActivityAggregateType.COMMENT);
    assertThat(outbox.getAggregateId()).isEqualTo(commentId);
    assertThat(outbox.getPayload()).isEqualTo(payloadJson);
  }

  @Test
  @DisplayName("기사 조회 이벤트를 ARTICLE_VIEWED Outbox로 저장한다")
  void saveArticleViewed_savesArticleViewedOutbox() {
    // given
    UUID userId = UUID.randomUUID();
    UUID articleViewId = UUID.randomUUID();
    UUID articleId = UUID.randomUUID();
    Instant viewedAt = Instant.parse("2026-05-01T00:00:00Z");
    Instant publishedAt = Instant.parse("2026-04-30T00:00:00Z");
    String payloadJson = "{\"event\":\"article-viewed\"}";

    ArticleViewItem item = new ArticleViewItem(
        articleViewId,
        userId,
        viewedAt,
        articleId,
        ArticleSource.NAVER,
        "https://example.com",
        "기사 제목",
        publishedAt,
        "기사 요약",
        3L,
        10L
    );

    ArticleViewedPayload payload = ArticleViewedPayload.of(userId, item);

    // 기사 조회 payload 직렬화 결과를 미리 준비한다.
    given(payloadSerializer.toJson(payload)).willReturn(payloadJson);
    given(userActivityOutboxRepository.save(any(UserActivityOutbox.class)))
        .willAnswer(invocation -> invocation.getArgument(0));

    // when
    userActivityOutboxService.saveArticleViewed(userId, item);

    // then
    // ARTICLE_VIEWED 이벤트와 ARTICLE_VIEW aggregate로 저장되었는지 검증한다.
    ArgumentCaptor<UserActivityOutbox> captor = ArgumentCaptor.forClass(UserActivityOutbox.class);
    verify(userActivityOutboxRepository).save(captor.capture());

    UserActivityOutbox outbox = captor.getValue();
    assertThat(outbox.getEventType()).isEqualTo(UserActivityEventType.ARTICLE_VIEWED);
    assertThat(outbox.getAggregateType()).isEqualTo(UserActivityAggregateType.ARTICLE_VIEW);
    assertThat(outbox.getAggregateId()).isEqualTo(articleViewId);
    assertThat(outbox.getPayload()).isEqualTo(payloadJson);
  }

  @Test
  @DisplayName("댓글 삭제 이벤트를 COMMENT_DELETED Outbox로 저장한다")
  void saveCommentDeleted_savesCommentDeletedOutbox() {
    // given
    UUID userId = UUID.randomUUID();
    UUID commentId = UUID.randomUUID();
    String payloadJson = "{\"event\":\"comment-deleted\"}";

    CommentDeletedPayload payload = CommentDeletedPayload.of(userId, commentId);

    // 댓글 삭제 payload 직렬화 결과를 미리 준비한다.
    given(payloadSerializer.toJson(payload)).willReturn(payloadJson);
    given(userActivityOutboxRepository.save(any(UserActivityOutbox.class)))
        .willAnswer(invocation -> invocation.getArgument(0));

    // when
    userActivityOutboxService.saveCommentDeleted(userId, commentId);

    // then
    ArgumentCaptor<UserActivityOutbox> captor = ArgumentCaptor.forClass(UserActivityOutbox.class);
    verify(userActivityOutboxRepository).save(captor.capture());

    UserActivityOutbox outbox = captor.getValue();
    assertThat(outbox.getEventType()).isEqualTo(UserActivityEventType.COMMENT_DELETED);
    assertThat(outbox.getAggregateType()).isEqualTo(UserActivityAggregateType.COMMENT);
    assertThat(outbox.getAggregateId()).isEqualTo(commentId);
    assertThat(outbox.getPayload()).isEqualTo(payloadJson);
  }
}
