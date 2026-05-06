package com.springboot.monew.user.event;

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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@Slf4j
@RequiredArgsConstructor
public class UserActivityEventListener {

  private final UserActivityUpdateService userActivityUpdateService;

  // 회원가입 직후 사용자 활동 문서를 생성한다.
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handle(UserRegisteredEvent event) {
    executeSafely("UserRegisteredEvent", () ->
        userActivityUpdateService.createUserActivity(
            UserRegisteredPayload.of(event.user())
        )
    );
  }

  // 닉네임 변경 후 사용자 활동 문서의 닉네임을 갱신한다.
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handle(UserNicknameUpdatedEvent event) {
    executeSafely("UserNicknameUpdatedEvent", () ->
        userActivityUpdateService.updateUserNickname(
            UserNicknameUpdatedPayload.of(event)
        )
    );
  }

  // 관심사 키워드 수정 후 구독 중인 사용자들의 활동 문서 구독 정보를 갱신한다.
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handle(InterestUpdatedEvent event) {
    executeSafely("InterestUpdatedEvent", () ->
        userActivityUpdateService.updateSubscriptionInterest(
            InterestUpdatedPayload.of(event)
        )
    );
  }

  // 관심사 구독 후 사용자 활동 문서에 구독 내역을 추가한다.
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handle(InterestSubscribedEvent event) {
    executeSafely("InterestSubscribedEvent", () ->
        userActivityUpdateService.addSubscription(event.userId(), event.item())
    );
  }

  // 관심사 구독 취소 후 사용자 활동 문서에서 구독 내역을 제거한다.
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handle(InterestUnsubscribedEvent event) {
    executeSafely("InterestUnsubscribedEvent", () ->
        userActivityUpdateService.removeSubscription(
            InterestUnsubscribedPayload.of(event)
        )
    );
  }

  // 댓글 작성 후 사용자 활동 문서에 댓글 내역을 추가한다.
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handle(CommentCreatedEvent event) {
    executeSafely("CommentCreatedEvent", () ->
        userActivityUpdateService.addComment(event.userId(), event.item())
    );
  }

  // 댓글 수정 후 사용자 활동 문서의 댓글 내역을 갱신한다.
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handle(CommentUpdatedEvent event) {
    executeSafely("CommentUpdatedEvent", () ->
        userActivityUpdateService.updateComment(event.userId(), event.item())
    );
  }

  // 댓글 삭제 후 사용자 활동 문서에서 댓글 내역을 제거한다.
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handle(CommentDeletedEvent event) {
    executeSafely("CommentDeletedEvent", () ->
        userActivityUpdateService.removeComment(
            CommentDeletedPayload.of(event)
        )
    );
  }

  // 댓글 좋아요 후 사용자 활동 문서에 좋아요 내역을 추가한다.
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handle(CommentLikedEvent event) {
    executeSafely("CommentLikedEvent", () ->
        userActivityUpdateService.addCommentLike(event.userId(), event.item())
    );
  }

  // 댓글 좋아요 취소 후 사용자 활동 문서에서 좋아요 내역을 제거한다.
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handle(CommentUnlikedEvent event) {
    executeSafely("CommentUnlikedEvent", () ->
        userActivityUpdateService.removeCommentLike(
            CommentUnlikedPayload.of(event)
        )
    );
  }

  // 댓글 좋아요 수 변경 후 사용자 활동 문서의 좋아요 수를 갱신한다.
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handle(CommentLikeCountUpdatedEvent event) {
    executeSafely("CommentLikeCountUpdatedEvent", () ->
        userActivityUpdateService.updateCommentLikeCount(
            CommentLikeCountUpdatedPayload.of(event)
        )
    );
  }

  // 기사 조회 후 사용자 활동 문서에 기사 조회 내역을 추가한다.
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handle(ArticleViewedEvent event) {
    executeSafely("ArticleViewedEvent", () ->
        userActivityUpdateService.addArticleView(event.userId(), event.item())
    );
  }

  // 즉시 반영에 실패해도 예외를 삼키고 Outbox 스케줄러가 재처리할 수 있도록 로그만 남긴다.
  private void executeSafely(String eventName, Runnable action) {
    try {
      action.run();
    } catch (Exception e) {
      log.error("사용자 활동 즉시 반영 실패 - event={}", eventName, e);
    }
  }
}
