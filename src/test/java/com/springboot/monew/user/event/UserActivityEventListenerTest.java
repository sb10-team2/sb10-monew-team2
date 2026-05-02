package com.springboot.monew.user.event;

import static org.mockito.Mockito.verify;

import com.springboot.monew.newsarticles.enums.ArticleSource;
import com.springboot.monew.user.document.UserActivityDocument.ArticleViewItem;
import com.springboot.monew.user.document.UserActivityDocument.CommentItem;
import com.springboot.monew.user.document.UserActivityDocument.CommentLikeItem;
import com.springboot.monew.user.document.UserActivityDocument.SubscriptionItem;
import com.springboot.monew.user.entity.User;
import com.springboot.monew.user.event.articleView.ArticleViewedEvent;
import com.springboot.monew.user.event.comment.CommentCreatedEvent;
import com.springboot.monew.user.event.comment.CommentDeletedEvent;
import com.springboot.monew.user.event.comment.CommentLikeCountUpdatedEvent;
import com.springboot.monew.user.event.comment.CommentLikedEvent;
import com.springboot.monew.user.event.comment.CommentUnlikedEvent;
import com.springboot.monew.user.event.comment.CommentUpdatedEvent;
import com.springboot.monew.user.event.interest.InterestSubscribedEvent;
import com.springboot.monew.user.event.interest.InterestUnsubscribedEvent;
import com.springboot.monew.user.event.interest.InterestUpdatedEvent;
import com.springboot.monew.user.event.user.UserNicknameUpdatedEvent;
import com.springboot.monew.user.event.user.UserRegisteredEvent;
import com.springboot.monew.user.outbox.payload.comment.CommentDeletedPayload;
import com.springboot.monew.user.outbox.payload.commentlike.CommentLikeCountUpdatedPayload;
import com.springboot.monew.user.outbox.payload.commentlike.CommentUnlikedPayload;
import com.springboot.monew.user.outbox.payload.interest.InterestUnsubscribedPayload;
import com.springboot.monew.user.outbox.payload.interest.InterestUpdatedPayload;
import com.springboot.monew.user.outbox.payload.user.UserNicknameUpdatedPayload;
import com.springboot.monew.user.outbox.payload.user.UserRegisteredPayload;
import com.springboot.monew.user.service.UserActivityUpdateService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class UserActivityEventListenerTest {

  @Mock
  private UserActivityUpdateService userActivityUpdateService;

  @InjectMocks
  private UserActivityEventListener userActivityEventListener;

  @Test
  @DisplayName("회원가입 이벤트를 수신하면 사용자 활동 문서를 생성한다")
  void handle_UserRegisteredEvent() {
    // given
    User user = new User("test@example.com", "tester", "password");

    UserRegisteredEvent event = new UserRegisteredEvent(user);

    // when
    userActivityEventListener.handle(event);

    // then
    verify(userActivityUpdateService).createUserActivity(UserRegisteredPayload.of(user));
  }

  @Test
  @DisplayName("닉네임 수정 이벤트를 수신하면 사용자 활동 문서의 닉네임을 갱신한다")
  void handle_UserNicknameUpdatedEvent() {
    // given
    UUID userId = UUID.randomUUID();
    String nickname = "newNickname";
    UserNicknameUpdatedEvent event = new UserNicknameUpdatedEvent(userId, nickname);

    // when
    userActivityEventListener.handle(event);

    // then
    verify(userActivityUpdateService).updateUserNickname(UserNicknameUpdatedPayload.of(event));
  }

  @Test
  @DisplayName("관심사 수정 이벤트를 수신하면 구독 중인 활동 문서의 키워드를 갱신한다")
  void handle_InterestUpdatedEvent() {
    // given
    UUID interestId = UUID.randomUUID();
    List<String> keywords = List.of("주식", "채권");
    InterestUpdatedEvent event = new InterestUpdatedEvent(interestId, keywords);

    // when
    userActivityEventListener.handle(event);

    // then
    verify(userActivityUpdateService).updateSubscriptionInterest(InterestUpdatedPayload.of(event));
  }

  @Test
  @DisplayName("관심사 구독 이벤트를 수신하면 활동 문서에 구독 내역을 추가한다")
  void handle_InterestSubscribedEvent() {
    // given
    UUID userId = UUID.randomUUID();
    SubscriptionItem item = new SubscriptionItem(
        UUID.randomUUID(),
        UUID.randomUUID(),
        "금융",
        List.of("주식", "채권"),
        Instant.now()
    );
    InterestSubscribedEvent event = new InterestSubscribedEvent(userId, item);

    // when
    userActivityEventListener.handle(event);

    // then
    verify(userActivityUpdateService).addSubscription(userId, item);
  }

  @Test
  @DisplayName("관심사 구독 취소 이벤트를 수신하면 활동 문서에서 구독 내역을 제거한다")
  void handle_InterestUnsubscribedEvent() {
    // given
    UUID userId = UUID.randomUUID();
    UUID interestId = UUID.randomUUID();
    InterestUnsubscribedEvent event = new InterestUnsubscribedEvent(userId, interestId);

    // when
    userActivityEventListener.handle(event);

    // then
    verify(userActivityUpdateService).removeSubscription(InterestUnsubscribedPayload.of(event));
  }

  @Test
  @DisplayName("댓글 생성 이벤트를 수신하면 활동 문서에 댓글 내역을 추가한다")
  void handle_CommentCreatedEvent() {
    // given
    UUID userId = UUID.randomUUID();
    CommentItem item = new CommentItem(
        UUID.randomUUID(),
        UUID.randomUUID(),
        "기사 제목",
        userId,
        "tester",
        "댓글 내용",
        0L,
        Instant.now()
    );
    CommentCreatedEvent event = new CommentCreatedEvent(userId, item);

    // when
    userActivityEventListener.handle(event);

    // then
    verify(userActivityUpdateService).addComment(userId, item);
  }

  @Test
  @DisplayName("댓글 수정 이벤트를 수신하면 활동 문서의 댓글 내역을 갱신한다")
  void handle_CommentUpdatedEvent() {
    // given
    UUID userId = UUID.randomUUID();
    CommentItem item = new CommentItem(
        UUID.randomUUID(),
        UUID.randomUUID(),
        "기사 제목",
        userId,
        "tester",
        "수정된 댓글",
        1L,
        Instant.now()
    );
    CommentUpdatedEvent event = new CommentUpdatedEvent(userId, item);

    // when
    userActivityEventListener.handle(event);

    // then
    verify(userActivityUpdateService).updateComment(userId, item);
  }

  @Test
  @DisplayName("댓글 삭제 이벤트를 수신하면 활동 문서에서 댓글 내역을 제거한다")
  void handle_CommentDeletedEvent() {
    // given
    UUID userId = UUID.randomUUID();
    UUID commentId = UUID.randomUUID();
    CommentDeletedEvent event = new CommentDeletedEvent(userId, commentId);

    // when
    userActivityEventListener.handle(event);

    // then
    verify(userActivityUpdateService).removeComment(CommentDeletedPayload.of(event));
  }

  @Test
  @DisplayName("댓글 좋아요 이벤트를 수신하면 활동 문서에 좋아요 내역을 추가한다")
  void handle_CommentLikedEvent() {
    // given
    UUID userId = UUID.randomUUID();
    CommentLikeItem item = new CommentLikeItem(
        UUID.randomUUID(),
        Instant.now(),
        UUID.randomUUID(),
        UUID.randomUUID(),
        "기사 제목",
        UUID.randomUUID(),
        "commentWriter",
        "댓글 내용",
        3L,
        Instant.now()
    );
    CommentLikedEvent event = new CommentLikedEvent(userId, item);

    // when
    userActivityEventListener.handle(event);

    // then
    verify(userActivityUpdateService).addCommentLike(userId, item);
  }

  @Test
  @DisplayName("댓글 좋아요 취소 이벤트를 수신하면 활동 문서에서 좋아요 내역을 제거한다")
  void handle_CommentUnlikedEvent() {
    // given
    UUID userId = UUID.randomUUID();
    UUID commentId = UUID.randomUUID();
    CommentUnlikedEvent event = new CommentUnlikedEvent(userId, commentId);

    // when
    userActivityEventListener.handle(event);

    // then
    verify(userActivityUpdateService).removeCommentLike(CommentUnlikedPayload.of(event));
  }

  @Test
  @DisplayName("댓글 좋아요 수 갱신 이벤트를 수신하면 활동 문서의 좋아요 수를 갱신한다")
  void handle_CommentLikeCountUpdatedEvent() {
    // given
    UUID userId = UUID.randomUUID();
    UUID commentId = UUID.randomUUID();
    long likeCount = 5L;
    CommentLikeCountUpdatedEvent event = new CommentLikeCountUpdatedEvent(userId, commentId, likeCount);

    // when
    userActivityEventListener.handle(event);

    // then
    verify(userActivityUpdateService).updateCommentLikeCount(CommentLikeCountUpdatedPayload.of(event));
  }

  @Test
  @DisplayName("기사 조회 이벤트를 수신하면 활동 문서에 기사 조회 내역을 추가한다")
  void handle_ArticleViewedEvent() {
    // given
    UUID userId = UUID.randomUUID();
    ArticleViewItem item = new ArticleViewItem(
        UUID.randomUUID(),
        userId,
        Instant.now(),
        UUID.randomUUID(),
        ArticleSource.NAVER,
        "https://example.com",
        "기사 제목",
        Instant.now(),
        "기사 요약",
        2L,
        10L
    );
    ArticleViewedEvent event = new ArticleViewedEvent(userId, item);

    // when
    userActivityEventListener.handle(event);

    // then
    verify(userActivityUpdateService).addArticleView(userId, item);
  }
}
