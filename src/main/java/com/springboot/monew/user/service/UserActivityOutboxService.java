package com.springboot.monew.user.service;

import com.springboot.monew.interest.entity.Interest;
import com.springboot.monew.interest.entity.Subscription;
import com.springboot.monew.user.document.UserActivityDocument.ArticleViewItem;
import com.springboot.monew.user.document.UserActivityDocument.CommentItem;
import com.springboot.monew.user.document.UserActivityDocument.CommentLikeItem;
import com.springboot.monew.user.entity.User;
import com.springboot.monew.user.outbox.UserActivityOutbox;
import com.springboot.monew.user.outbox.UserActivityOutboxPayloadSerializer;
import com.springboot.monew.user.outbox.enums.UserActivityAggregateType;
import com.springboot.monew.user.outbox.enums.UserActivityEventType;
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
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserActivityOutboxService {

  private final UserActivityOutboxRepository userActivityOutboxRepository;

  private final UserActivityOutboxPayloadSerializer payloadSerializer;

  // payload를 JSON으로 변환해 Outbox row로 저장한다.
  public UserActivityOutbox save(
      UserActivityEventType eventType,
      UserActivityAggregateType aggregateType,
      UUID aggregateId,
      Object payload
  ) {
    String payloadJson = payloadSerializer.toJson(payload);

    UserActivityOutbox outbox = new UserActivityOutbox(
        eventType,
        aggregateType,
        aggregateId,
        payloadJson,
        Instant.now()
    );

    return userActivityOutboxRepository.save(outbox);
  }

  // 회원가입 이벤트를 Outbox에 저장한다.
  @Transactional(propagation = Propagation.MANDATORY)
  public void saveUserRegistered(User user) {
    save(
        UserActivityEventType.USER_REGISTERED,
        UserActivityAggregateType.USER,
        user.getId(),
        UserRegisteredPayload.of(user)
    );
  }

  // 닉네임 수정 이벤트를 Outbox에 저장한다.
  @Transactional(propagation = Propagation.MANDATORY)
  public void saveUserNicknameUpdated(User user) {
    save(
        UserActivityEventType.USER_NICKNAME_UPDATED,
        UserActivityAggregateType.USER,
        user.getId(),
        UserNicknameUpdatedPayload.of(user)
    );
  }

  // 관심사 구독 이벤트를 Outbox에 저장한다.
  @Transactional(propagation = Propagation.MANDATORY)
  public void saveInterestSubscribed(Subscription subscription, List<String> keywords) {
    save(
        UserActivityEventType.INTEREST_SUBSCRIBED,
        UserActivityAggregateType.SUBSCRIPTION,
        subscription.getId(),
        InterestSubscribedPayload.of(subscription, keywords)
    );
  }

  // 관심사 구독 취소 이벤트를 Outbox에 저장한다.
  @Transactional(propagation = Propagation.MANDATORY)
  public void saveInterestUnsubscribed(UUID userId, UUID interestId) {
    save(
        UserActivityEventType.INTEREST_UNSUBSCRIBED,
        UserActivityAggregateType.INTEREST,
        interestId,
        InterestUnsubscribedPayload.of(userId, interestId)
    );
  }

  // 관심사 키워드 수정 이벤트를 Outbox에 저장한다.
  @Transactional(propagation = Propagation.MANDATORY)
  public void saveInterestUpdated(UUID interestId, List<String> keywords) {
    save(
        UserActivityEventType.INTEREST_UPDATED,
        UserActivityAggregateType.INTEREST,
        interestId,
        InterestUpdatedPayload.of(interestId, keywords)
    );
  }

  // 댓글 생성 이벤트를 Outbox에 저장한다.
  @Transactional(propagation = Propagation.MANDATORY)
  public void saveCommentCreated(UUID userId, CommentItem item) {
    save(
        UserActivityEventType.COMMENT_CREATED,
        UserActivityAggregateType.COMMENT,
        item.id(),
        CommentActivityPayload.of(userId, item)
    );
  }

  // 댓글 수정 이벤트를 Outbox에 저장한다.
  @Transactional(propagation = Propagation.MANDATORY)
  public void saveCommentUpdated(UUID userId, CommentItem item) {
    save(
        UserActivityEventType.COMMENT_UPDATED,
        UserActivityAggregateType.COMMENT,
        item.id(),
        CommentActivityPayload.of(userId, item)
    );
  }

  // 댓글 삭제 이벤트를 Outbox에 저장한다.
  @Transactional(propagation = Propagation.MANDATORY)
  public void saveCommentDeleted(UUID userId, UUID commentId) {
    save(
        UserActivityEventType.COMMENT_DELETED,
        UserActivityAggregateType.COMMENT,
        commentId,
        CommentDeletedPayload.of(userId, commentId)
    );
  }

  // 댓글 좋아요 이벤트를 Outbox에 저장한다.
  @Transactional(propagation = Propagation.MANDATORY)
  public void saveCommentLiked(UUID userId, CommentLikeItem item) {
    save(
        UserActivityEventType.COMMENT_LIKED,
        UserActivityAggregateType.COMMENT_LIKE,
        item.id(),
        CommentLikeActivityPayload.of(userId, item)
    );
  }

  // 댓글 좋아요 취소 이벤트를 Outbox에 저장한다.
  @Transactional(propagation = Propagation.MANDATORY)
  public void saveCommentUnliked(UUID userId, UUID commentId) {
    save(
        UserActivityEventType.COMMENT_UNLIKED,
        UserActivityAggregateType.COMMENT,
        commentId,
        CommentUnlikedPayload.of(userId, commentId)
    );
  }

  // 댓글 좋아요 수 변경 이벤트를 Outbox에 저장한다.
  @Transactional(propagation = Propagation.MANDATORY)
  public void saveCommentLikeCountUpdated(UUID userId, UUID commentId, long likeCount) {
    save(
        UserActivityEventType.COMMENT_LIKE_COUNT_UPDATED,
        UserActivityAggregateType.COMMENT,
        commentId,
        CommentLikeCountUpdatedPayload.of(userId, commentId, likeCount)
    );
  }

  // 기사 조회 이벤트를 Outbox에 저장한다.
  @Transactional(propagation = Propagation.MANDATORY)
  public void saveArticleViewed(UUID userId, ArticleViewItem item) {
    save(
        UserActivityEventType.ARTICLE_VIEWED,
        UserActivityAggregateType.ARTICLE_VIEW,
        item.id(),
        ArticleViewedPayload.of(userId, item)
    );
  }
}
