package com.springboot.monew.users.service;

import com.springboot.monew.users.document.UserActivityDocument;
import com.springboot.monew.users.document.UserActivityDocument.ArticleViewItem;
import com.springboot.monew.users.document.UserActivityDocument.CommentItem;
import com.springboot.monew.users.document.UserActivityDocument.CommentLikeItem;
import com.springboot.monew.users.document.UserActivityDocument.SubscriptionItem;
import com.springboot.monew.users.entity.User;
import com.springboot.monew.users.exception.UserErrorCode;
import com.springboot.monew.users.exception.UserException;
import com.springboot.monew.users.repository.UserActivityRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserActivityUpdateService {

  private final UserActivityRepository userActivityRepository;

  // 회원가입 성공 시 사용자 활동 내역 문서를 새로 생성한다.
  public void createUserActivity(User user) {
    UserActivityDocument document = new UserActivityDocument(
        user.getId(),
        user.getEmail(),
        user.getNickname(),
        user.getCreatedAt()
    );

    userActivityRepository.save(document);
  }

  // 사용자 닉네임이 변경되면 활동 내역 문서의 사용자 기본 정보도 갱신한다.
  public void updateUserNickname(UUID userId, String nickname) {
    UserActivityDocument document = getDocument(userId);
    document.updateNickname(nickname);
    userActivityRepository.save(document);
  }

  // 관심사 키워드가 변경되면 해당 관심사를 구독 중인 사용자들의 활동 내역 구독 정보를 갱신한다.
  @Transactional("mongoTransactionManager")
  public void updateSubscriptionInterest(UUID interestId, List<String> keywords) {
    List<UserActivityDocument> documents =
        userActivityRepository.findAllBySubscriptionsInterestId(interestId);

    documents.forEach(document ->
      document.updateSubscriptionInterest(interestId, keywords)
    );

    userActivityRepository.saveAll(documents);
  }


  // 관심사 구독 시 사용자 활동 문서에 구독 내역을 추가한다.
  public void addSubscription(UUID userId, SubscriptionItem item) {
    UserActivityDocument document = getDocument(userId);
    document.addSubscription(item);
    userActivityRepository.save(document);
  }

  // 관심사 구독 취소 시 사용자 활동 문서에서 구독 내역을 제거한다.
  public void removeSubscription(UUID userId, UUID interestId) {
    UserActivityDocument document = getDocument(userId);
    document.removeSubscription(interestId);
    userActivityRepository.save(document);
  }

  // 댓글 작성 시 사용자 활동 문서에 댓글 내역을 추가한다.
  public void addComment(UUID userId, CommentItem item) {
    UserActivityDocument document = getDocument(userId);
    document.addComment(item);
    userActivityRepository.save(document);
  }

  // 댓글 수정 시 사용자 활동 문서의 댓글 내역을 갱신한다.
  public void updateComment(UUID userId, CommentItem item) {
    UserActivityDocument document = getDocument(userId);
    document.updateComment(item);
    userActivityRepository.save(document);
  }

  // 댓글 삭제 시 사용자 활동 문서에서 댓글 내역을 제거한다.
  public void removeComment(UUID userId, UUID commentId) {
    UserActivityDocument document = getDocument(userId);
    document.removeComment(commentId);
    userActivityRepository.save(document);
  }

  // 댓글 좋아요 시 사용자 활동 문서에 좋아요 내역을 추가한다.
  public void addCommentLike(UUID userId, CommentLikeItem item) {
    UserActivityDocument document = getDocument(userId);
    document.addCommentLike(item);
    userActivityRepository.save(document);
  }

  // 댓글 좋아요 취소 시 사용자 활동 문서에서 좋아요 내역을 제거한다.
  public void removeCommentLike(UUID userId, UUID commentId) {
    UserActivityDocument document = getDocument(userId);
    document.removeCommentLike(commentId);
    userActivityRepository.save(document);
  }

  // 댓글 좋아요 수 변경 시 사용자 활동 문서의 댓글 좋아요 수를 갱신한다.
  public void updateCommentLikeCount(UUID userId, UUID commentId, long likeCount) {
    UserActivityDocument document = getDocument(userId);
    document.updateCommentLikeCount(commentId, likeCount);
    userActivityRepository.save(document);
  }

  // 기사 조회 시 사용자 활동 문서에 기사 조회 내역을 추가한다.
  public void addArticleView(UUID userId, ArticleViewItem item) {
    UserActivityDocument document = getDocument(userId);
    document.addArticleView(item);
    userActivityRepository.save(document);
  }

  // 사용자 활동 내역 문서를 조회한다. 없으면 회원가입 시 문서 생성이 누락된 상태로 보고 예외를 던진다.
  private UserActivityDocument getDocument(UUID userId) {
    return userActivityRepository.findById(userId)
        .orElseThrow(() -> new UserException(
            UserErrorCode.USER_NOT_FOUND,
            Map.of("userId", userId)
        ));
  }

}
