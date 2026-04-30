package com.springboot.monew.user.outbox;

import com.springboot.monew.user.document.UserActivityDocument.ArticleViewItem;
import com.springboot.monew.user.document.UserActivityDocument.CommentItem;
import com.springboot.monew.user.document.UserActivityDocument.CommentLikeItem;
import com.springboot.monew.user.document.UserActivityDocument.SubscriptionItem;
import com.springboot.monew.user.outbox.enums.UserActivityEventType;
import com.springboot.monew.user.outbox.enums.UserActivityOutboxStatus;
import com.springboot.monew.user.outbox.payload.articleview.ArticleViewedPayload;
import com.springboot.monew.user.outbox.payload.comment.CommentActivityPayload;
import com.springboot.monew.user.outbox.payload.comment.CommentDeletedPayload;
import com.springboot.monew.user.outbox.payload.commentlike.CommentLikeActivityPayload;
import com.springboot.monew.user.outbox.payload.commentlike.CommentLikeCountUpdatedPayload;
import com.springboot.monew.user.outbox.payload.commentlike.CommentUnlikedPayload;
import com.springboot.monew.user.outbox.payload.interest.InterestSubscribedPayload;
import com.springboot.monew.user.outbox.payload.interest.InterestUnsubscribedPayload;
import com.springboot.monew.user.outbox.payload.interest.InterestUpdatedPayload;
import com.springboot.monew.user.outbox.payload.user.UserNicknameUpdatedPayload;
import com.springboot.monew.user.outbox.payload.user.UserRegisteredPayload;
import com.springboot.monew.user.repository.UserActivityOutboxRepository;
import com.springboot.monew.user.service.UserActivityUpdateService;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserActivityOutboxProcessor {

  private final UserActivityOutboxRepository userActivityOutboxRepository;
  private final UserActivityOutboxPayloadSerializer payloadSerializer;
  private final UserActivityUpdateService userActivityUpdateService;

  // 미처리 Outbox 이벤트를 발생 순서대로 처리한다.
  @Transactional
  public void processPendingEvents() {
    List<UserActivityOutbox> outboxes =
        userActivityOutboxRepository.findAllByStatusOrderByOccurredAtAsc(
            UserActivityOutboxStatus.PENDING
        );

    for (UserActivityOutbox outbox : outboxes) {
      processSingleEvent(outbox);
    }
  }

  // Outbox 이벤트 한 건을 처리하고 결과 상태를 반영한다.
  private void processSingleEvent(UserActivityOutbox outbox) {
    try {
      handle(outbox);
      outbox.markProcessed(Instant.now());
      log.info(
          "사용자 활동 Outbox 처리 완료 - outboxId={}, eventType={}",
          outbox.getId(),
          outbox.getEventType()
      );
    } catch (Exception e) {
      outbox.markFailed(e.getMessage());
      log.error(
          "사용자 활동 Outbox 처리 실패 - outboxId={}, eventType={}",
          outbox.getId(),
          outbox.getEventType(),
          e
      );
    }
  }

  // eventType에 따라 payload를 역직렬화하고 사용자 활동 문서 갱신 로직을 호출한다.
  private void handle(UserActivityOutbox outbox) {
    UserActivityEventType eventType = outbox.getEventType();
    String payloadJson = outbox.getPayload();

    switch (eventType) {
      case USER_REGISTERED -> {
        UserRegisteredPayload payload =
            payloadSerializer.fromJson(payloadJson, UserRegisteredPayload.class);
        userActivityUpdateService.createUserActivity(
            payload.userId(),
            payload.email(),
            payload.nickname(),
            payload.createdAt()
        );
      }

      case USER_NICKNAME_UPDATED -> {
        UserNicknameUpdatedPayload payload =
            payloadSerializer.fromJson(payloadJson, UserNicknameUpdatedPayload.class);
        userActivityUpdateService.updateUserNickname(
            payload.userId(),
            payload.nickname()
        );
      }

      case INTEREST_SUBSCRIBED -> {
        InterestSubscribedPayload payload =
            payloadSerializer.fromJson(payloadJson, InterestSubscribedPayload.class);

        SubscriptionItem item = new SubscriptionItem(
            payload.subscriptionId(),
            payload.interestId(),
            payload.interestName(),
            payload.interestKeywords(),
            payload.createdAt()
        );

        userActivityUpdateService.addSubscription(payload.userId(), item);
      }

      case INTEREST_UNSUBSCRIBED -> {
        InterestUnsubscribedPayload payload =
            payloadSerializer.fromJson(payloadJson, InterestUnsubscribedPayload.class);
        userActivityUpdateService.removeSubscription(
            payload.userId(),
            payload.interestId()
        );
      }

      case INTEREST_UPDATED -> {
        InterestUpdatedPayload payload =
            payloadSerializer.fromJson(payloadJson, InterestUpdatedPayload.class);
        userActivityUpdateService.updateSubscriptionInterest(
            payload.interestId(),
            payload.keywords()
        );
      }

      case COMMENT_CREATED -> {
        CommentActivityPayload payload =
            payloadSerializer.fromJson(payloadJson, CommentActivityPayload.class);

        CommentItem item = new CommentItem(
            payload.commentId(),
            payload.articleId(),
            payload.articleTitle(),
            payload.commentUserId(),
            payload.commentUserNickname(),
            payload.content(),
            payload.likeCount(),
            payload.createdAt()
        );

        userActivityUpdateService.addComment(payload.userId(), item);
      }

      case COMMENT_UPDATED -> {
        CommentActivityPayload payload =
            payloadSerializer.fromJson(payloadJson, CommentActivityPayload.class);

        CommentItem item = new CommentItem(
            payload.commentId(),
            payload.articleId(),
            payload.articleTitle(),
            payload.commentUserId(),
            payload.commentUserNickname(),
            payload.content(),
            payload.likeCount(),
            payload.createdAt()
        );

        userActivityUpdateService.updateComment(payload.userId(), item);
      }

      case COMMENT_DELETED -> {
        CommentDeletedPayload payload =
            payloadSerializer.fromJson(payloadJson, CommentDeletedPayload.class);
        userActivityUpdateService.removeComment(
            payload.userId(),
            payload.commentId()
        );
      }

      case COMMENT_LIKED -> {
        CommentLikeActivityPayload payload =
            payloadSerializer.fromJson(payloadJson, CommentLikeActivityPayload.class);

        CommentLikeItem item = new CommentLikeItem(
            payload.commentLikeId(),
            payload.createdAt(),
            payload.commentId(),
            payload.articleId(),
            payload.articleTitle(),
            payload.commentUserId(),
            payload.commentUserNickname(),
            payload.commentContent(),
            payload.commentLikeCount(),
            payload.commentCreatedAt()
        );

        userActivityUpdateService.addCommentLike(payload.userId(), item);
      }

      case COMMENT_UNLIKED -> {
        CommentUnlikedPayload payload =
            payloadSerializer.fromJson(payloadJson, CommentUnlikedPayload.class);
        userActivityUpdateService.removeCommentLike(
            payload.userId(),
            payload.commentId()
        );
      }

      case COMMENT_LIKE_COUNT_UPDATED -> {
        CommentLikeCountUpdatedPayload payload =
            payloadSerializer.fromJson(payloadJson, CommentLikeCountUpdatedPayload.class);
        userActivityUpdateService.updateCommentLikeCount(
            payload.userId(),
            payload.commentId(),
            payload.likeCount()
        );
      }

      case ARTICLE_VIEWED -> {
        ArticleViewedPayload payload =
            payloadSerializer.fromJson(payloadJson, ArticleViewedPayload.class);

        ArticleViewItem item = new ArticleViewItem(
            payload.articleViewId(),
            payload.userId(),
            payload.createdAt(),
            payload.articleId(),
            payload.source(),
            payload.sourceUrl(),
            payload.articleTitle(),
            payload.articlePublishedDate(),
            payload.articleSummary(),
            payload.articleCommentCount(),
            payload.articleViewCount()
        );

        userActivityUpdateService.addArticleView(payload.userId(), item);
      }

      default -> throw new IllegalStateException("지원하지 않는 Outbox 이벤트 타입이다. eventType=" + eventType);
    }
  }
}