package com.springboot.monew.users.event;

import com.springboot.monew.users.event.articleView.ArticleViewedEvent;
import com.springboot.monew.users.event.comment.CommentCreatedEvent;
import com.springboot.monew.users.event.comment.CommentDeletedEvent;
import com.springboot.monew.users.event.comment.CommentLikeCountUpdatedEvent;
import com.springboot.monew.users.event.comment.CommentLikedEvent;
import com.springboot.monew.users.event.comment.CommentUnlikedEvent;
import com.springboot.monew.users.event.comment.CommentUpdatedEvent;
import com.springboot.monew.users.event.interest.InterestSubscribedEvent;
import com.springboot.monew.users.event.interest.InterestUnsubscribedEvent;
import com.springboot.monew.users.event.user.UserNicknameUpdatedEvent;
import com.springboot.monew.users.event.user.UserRegisteredEvent;
import com.springboot.monew.users.service.UserActivityUpdateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class UserActivityEventListener {

  private final UserActivityUpdateService userActivityUpdateService;

  // 회원가입 이벤트를 수신하여 사용자 활동 문서 생성
  @TransactionalEventListener
  public void handle(UserRegisteredEvent event) {
    userActivityUpdateService.createUserActivity(event.user());
  }

  // 닉네임 변경 이벤트를 수신하여 사용자 활동 문서의 닉네임을 갱신
  @TransactionalEventListener
  public void handle(UserNicknameUpdatedEvent event) {
    userActivityUpdateService.updateUserNickname(event.userId(), event.nickname());
  }

  // 관심사 구독 이벤트를 수신하여 사용자 활동 문서에 구독 내역을 추가
  @TransactionalEventListener
  public void handle(InterestSubscribedEvent event) {
    userActivityUpdateService.addSubscription(event.userId(), event.item());
  }

  // 관심사 구독 취소 이벤트를 수신하여 사용자 활동 문서에서 구독 내역을 제거
  @TransactionalEventListener
  public void handle(InterestUnsubscribedEvent event) {
    userActivityUpdateService.removeSubscription(event.userId(), event.interestId());
  }

  // 댓글 작성 이벤트를 수신하여 사용자 활동 문서에 댓글 내역을 추가
  @TransactionalEventListener
  public void handle(CommentCreatedEvent event) {
    userActivityUpdateService.addComment(event.userId(), event.item());
  }


  // 댓글 수정 이벤트를 수신하여 사용자 활동 문서의 댓글 내역을 갱신
  @TransactionalEventListener
  public void handle(CommentUpdatedEvent event) {
    userActivityUpdateService.updateComment(event.userId(), event.item());
  }

  // 댓글 삭제 이벤트를 수신하여 사용자 활동 문서에서 댓글 내역을 제거
  @TransactionalEventListener
  public void handle(CommentDeletedEvent event) {
    userActivityUpdateService.removeComment(event.userId(), event.commentId());
  }

  // 댓글 좋아요 이벤트를 수신하여 사용자 활동 문서에 좋아요 내역을 추가
  @TransactionalEventListener
  public void handle(CommentLikedEvent event) {
    userActivityUpdateService.addCommentLike(event.userId(), event.item());
  }

  // 댓글 좋아요 취소 이벤트를 수신하여 사용자 활동 문서에서 좋아요 내역을 제거한다.
  @TransactionalEventListener
  public void handle(CommentUnlikedEvent event) {
    userActivityUpdateService.removeCommentLike(event.userId(), event.commentId());
  }

  // 댓글 좋아요 수 변경 이벤트를 수신하여 사용자 활동 문서의 댓글 좋아요 수를 갱신
  @TransactionalEventListener
  public void handle(CommentLikeCountUpdatedEvent event) {
    userActivityUpdateService.updateCommentLikeCount(
        event.userId(),
        event.commentId(),
        event.likeCount()
    );
  }

  // 기사 조회 이벤트를 수신하여 사용자 활동 문서에 기사 조회 내역을 추가
  @TransactionalEventListener
  public void handle(ArticleViewedEvent event) {
    userActivityUpdateService.addArticleView(event.userId(), event.item());
  }
}
