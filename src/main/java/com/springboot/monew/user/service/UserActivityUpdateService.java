package com.springboot.monew.user.service;

import com.springboot.monew.user.document.UserActivityDocument;
import com.springboot.monew.user.document.UserActivityDocument.ArticleViewItem;
import com.springboot.monew.user.document.UserActivityDocument.CommentItem;
import com.springboot.monew.user.document.UserActivityDocument.CommentLikeItem;
import com.springboot.monew.user.document.UserActivityDocument.SubscriptionItem;
import com.springboot.monew.user.exception.UserErrorCode;
import com.springboot.monew.user.exception.UserException;
import com.springboot.monew.user.outbox.payload.comment.CommentDeletedPayload;
import com.springboot.monew.user.outbox.payload.commentlike.CommentLikeCountUpdatedPayload;
import com.springboot.monew.user.outbox.payload.commentlike.CommentUnlikedPayload;
import com.springboot.monew.user.outbox.payload.interest.InterestUnsubscribedPayload;
import com.springboot.monew.user.outbox.payload.interest.InterestUpdatedPayload;
import com.springboot.monew.user.outbox.payload.user.UserNicknameUpdatedPayload;
import com.springboot.monew.user.outbox.payload.user.UserRegisteredPayload;
import com.springboot.monew.user.repository.UserActivityRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserActivityUpdateService {

  private final UserActivityRepository userActivityRepository;

  // 회원가입 성공 시 사용자 활동 내역 문서를 새로 생성한다.
  public void createUserActivity(UserRegisteredPayload payload) {
    if (userActivityRepository.existsById(payload.userId())) {
      log.info("사용자 활동 문서가 이미 존재하여 생성 생략 - userId={}", payload.userId());
      return;
    }
    UserActivityDocument document = new UserActivityDocument(
        payload.userId(),
        payload.email(),
        payload.nickname(),
        payload.createdAt()
    );

    userActivityRepository.save(document);
    log.info("사용자 활동 문서 생성 완료 - userId={}", payload.userId());
  }

  // 사용자 닉네임이 변경되면 활동 내역 문서의 사용자 기본 정보도 갱신한다.
  public void updateUserNickname(UserNicknameUpdatedPayload payload) {
    UserActivityDocument document = getDocument(payload.userId());
    document.updateNickname(payload.nickname());
    userActivityRepository.save(document);
    log.info("사용자 활동 닉네임 갱신 완료 - userId={}", payload.userId());
  }

  // 관심사 키워드가 변경되면 해당 관심사를 구독 중인 사용자들의 활동 내역 구독 정보를 갱신한다.
  @Transactional("mongoTransactionManager")
  public void updateSubscriptionInterest(InterestUpdatedPayload payload) {
    List<UserActivityDocument> documents =
        userActivityRepository.findAllBySubscriptionsInterestId(payload.interestId());

    documents.forEach(document ->
        document.updateSubscriptionInterest(payload.interestId(), payload.keywords())
    );

    userActivityRepository.saveAll(documents);
    log.info("사용자 활동 관심사 키워드 일괄 갱신 완료 - interestId={}, documentCount={}", payload.interestId(),
        documents.size());
  }


  // 관심사 구독 시 사용자 활동 문서에 구독 내역을 추가한다.
  public void addSubscription(UUID userId, SubscriptionItem item) {
    UserActivityDocument document = getDocument(userId);
    document.addSubscription(item);
    userActivityRepository.save(document);
    log.info("사용자 활동 구독 추가 완료 - userId={}, interestId={}", userId, item.interestId());
  }

  // 관심사 구독 취소 시 사용자 활동 문서에서 구독 내역을 제거한다.
  public void removeSubscription(InterestUnsubscribedPayload payload) {
    UserActivityDocument document = getDocument(payload.userId());
    document.removeSubscription(payload.interestId());
    userActivityRepository.save(document);
    log.info("사용자 활동 구독 제거 완료 - userId={}, interestId={}", payload.userId(), payload.interestId());
  }

  // 댓글 작성 시 사용자 활동 문서에 댓글 내역을 추가한다.
  public void addComment(UUID userId, CommentItem item) {
    UserActivityDocument document = getDocument(userId);
    document.addComment(item);
    userActivityRepository.save(document);
    log.info("사용자 활동 댓글 추가 완료 - userId={}, commentId={}", userId, item.id());
  }

  // 댓글 수정 시 사용자 활동 문서의 댓글 내역을 갱신한다.
  public void updateComment(UUID userId, CommentItem item) {
    UserActivityDocument document = getDocument(userId);
    document.updateComment(item);
    userActivityRepository.save(document);
    log.info("사용자 활동 댓글 수정 완료 - userId={}, commentId={}", userId, item.id());
  }

  // 댓글 삭제 시 사용자 활동 문서에서 댓글 내역을 제거한다.
  public void removeComment(CommentDeletedPayload payload) {
    UserActivityDocument document = getDocument(payload.userId());
    document.removeComment(payload.commentId());
    userActivityRepository.save(document);
    log.info("사용자 활동 댓글 제거 완료 - userId={}, commentId={}", payload.userId(), payload.commentId());
  }

  // 댓글 좋아요 시 사용자 활동 문서에 좋아요 내역을 추가한다.
  public void addCommentLike(UUID userId, CommentLikeItem item) {
    UserActivityDocument document = getDocument(userId);
    document.addCommentLike(item);
    userActivityRepository.save(document);
    log.info("사용자 활동 댓글 좋아요 추가 완료 - userId={}, commentId={}", userId, item.commentId());
  }

  // 댓글 좋아요 취소 시 사용자 활동 문서에서 좋아요 내역을 제거한다.
  public void removeCommentLike(CommentUnlikedPayload payload) {
    UserActivityDocument document = getDocument(payload.userId());
    document.removeCommentLike(payload.commentId());
    userActivityRepository.save(document);
    log.info("사용자 활동 댓글 좋아요 제거 완료 - userId={}, commentId={}", payload.userId(), payload.commentId());
  }

  // 댓글 좋아요 수 변경 시 사용자 활동 문서의 댓글 좋아요 수를 갱신한다.
  public void updateCommentLikeCount(CommentLikeCountUpdatedPayload payload) {
    UserActivityDocument document = getDocument(payload.userId());
    document.updateCommentLikeCount(payload.commentId(), payload.likeCount());
    userActivityRepository.save(document);
    log.info("사용자 활동 댓글 좋아요 수 갱신 완료 - userId={}, commentId={}, likeCount={}", payload.userId(), payload.commentId(),
        payload.likeCount());
  }

  // 기사 조회 시 사용자 활동 문서에 기사 조회 내역을 추가한다.
  public void addArticleView(UUID userId, ArticleViewItem item) {
    UserActivityDocument document = getDocument(userId);
    document.addArticleView(item);
    userActivityRepository.save(document);
    log.info("사용자 활동 기사 조회 추가 완료 - userId={}, articleId={}", userId, item.articleId());
  }

  // 사용자 활동 내역 문서를 조회한다. 없으면 회원가입 시 문서 생성이 누락된 상태로 보고 예외를 던진다.
  private UserActivityDocument getDocument(UUID userId) {
    return userActivityRepository.findById(userId)
        .orElseThrow(() -> {
          log.warn("사용자 활동 문서 조회 실패: 문서를 찾을 수 없음 - userId={}", userId);
          return new UserException(
              UserErrorCode.USER_ACTIVITY_NOT_FOUND,
              Map.of("userId", userId)
          );
        });
  }

}
