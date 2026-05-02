package com.springboot.monew.comment.service;

import com.springboot.monew.comment.dto.CommentLikeDto;
import com.springboot.monew.comment.entity.Comment;
import com.springboot.monew.comment.entity.CommentLike;
import com.springboot.monew.comment.exception.CommentErrorCode;
import com.springboot.monew.comment.exception.CommentException;
import com.springboot.monew.comment.mapper.CommentLikeMapper;
import com.springboot.monew.comment.repository.CommentLikeRepository;
import com.springboot.monew.comment.repository.CommentRepository;
import com.springboot.monew.notification.event.CommentLikeNotificationEvent;
import com.springboot.monew.user.document.UserActivityDocument.CommentLikeItem;
import com.springboot.monew.user.entity.User;
import com.springboot.monew.user.event.comment.CommentLikeCountUpdatedEvent;
import com.springboot.monew.user.event.comment.CommentLikedEvent;
import com.springboot.monew.user.event.comment.CommentUnlikedEvent;
import com.springboot.monew.user.exception.UserErrorCode;
import com.springboot.monew.user.exception.UserException;
import com.springboot.monew.user.repository.UserRepository;
import com.springboot.monew.user.service.UserActivityOutboxService;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentLikeService {
  private final CommentLikeRepository commentLikeRepository;
  private final CommentRepository commentRepository;
  private final CommentLikeMapper commentLikeMapper;
  private final UserRepository userRepository;
  private final ApplicationEventPublisher eventPublisher;
  private final UserActivityOutboxService userActivityOutboxService;

  // 좋아요
  @Transactional
  public CommentLikeDto like(UUID commentId, UUID userId) {
    Comment comment = getActiveComment(commentId);
    getActiveUser(userId); // 논리 삭제 사용자 여부를 먼저 검증

    // 중복 좋아요 check
    if (commentLikeRepository.existsByCommentIdAndUserId(commentId, userId)) {
      throw new CommentException(
          CommentErrorCode.COMMENT_LIKE_ALREADY_EXISTS,
          Map.of("commentId", commentId, "userId", userId));
    }

    log.debug("likeCount 증가 전 - commentId: {}, likeCount: {}", commentId, comment.getLikeCount());
    commentRepository.incrementLikeCount(comment.getId());
    // comment 재조회 , incrementLikeCount 영속성 컨텍스트 초기화가 일어나 좋아요가 증가한 comment를 받으려면 재조회가 필요하다고 판단
    Comment refreshed = commentRepository.findByIdAndIsDeletedFalse(commentId)
        .orElseThrow(() -> new CommentException(CommentErrorCode.COMMENT_NOT_FOUND, Map.of("commentId", commentId)));
    // bulk update 후 영속성 컨텍스트가 초기화되므로, CommentLike 저장에 사용할 user는 clear 이후에 조회한다.
    User user = getActiveUser(userId);
    CommentLike commentLike = new CommentLike(refreshed, user);
    commentLikeRepository.save(commentLike);

    // Outbox와 이벤트에서 같은 값을 쓰도록 한 번만 매핑한다.
    CommentLikeItem commentLikeItem = commentLikeMapper.toCommentLikeItem(commentLike);

    // 좋아요를 누른 사용자의 활동 문서에 댓글 좋아요 활동을 반영하기 위한 Outbox 이벤트를 저장한다.
    userActivityOutboxService.saveCommentLiked(userId, commentLikeItem);

    // 좋아요 대상 댓글 작성자의 활동 문서에 저장된 댓글 좋아요 수를 갱신하기 위한 Outbox 이벤트를 저장한다.
    userActivityOutboxService.saveCommentLikeCountUpdated(
        refreshed.getUser().getId(),
        refreshed.getId(),
        refreshed.getLikeCount()
    );

    // 좋아요를 누른 사용자의 활동 문서에 댓글 좋아요 활동을 추가한다.
    eventPublisher.publishEvent(new CommentLikedEvent(userId, commentLikeItem));

    // 좋아요 대상 댓글 작성자의 활동 문서에 저장된 댓글 좋아요 수를 갱신한다.
    eventPublisher.publishEvent(
        new CommentLikeCountUpdatedEvent(
            refreshed.getUser().getId(),
            refreshed.getId(),
            refreshed.getLikeCount())
    );

    log.debug("likeCount 증가 후 - commentId: {}, likeCount: {}", commentId, refreshed.getLikeCount());
    log.info("좋아요 등록 완료 - commentId: {}, userId: {}", commentId, userId);
    eventPublisher.publishEvent(CommentLikeNotificationEvent.from(user, commentLike));
    return commentLikeMapper.toCommentLikeDto(commentLike);
  }

  // 댓글 좋아요 취소
  @Transactional
  public void unlike(UUID commentId, UUID userId) {
    Comment comment = getActiveComment(commentId);
    User user = getActiveUser(userId);

    // 좋아요 존재 확인
    CommentLike commentLike =
        commentLikeRepository
            .findCommentLikeByCommentAndUser(comment, user)
            .orElseThrow(
                () ->
                    new CommentException(
                        CommentErrorCode.COMMENT_LIKE_NOT_FOUND, Map.of("commentId", commentId)));

    // 좋아요 삭제(하드 딜릿) -> 좋아요 취소 시 이력에서 바로 삭제되므로 하드딜릿이 맞다고 판단
    commentLikeRepository.delete(commentLike);
    commentRepository.decrementLikeCount(comment.getId());

    // 취소된 좋아요 수를 반영하기 위해 댓글을 다시 조회한다.
    Comment refreshed = commentRepository.findByIdAndIsDeletedFalse(commentId)
        .orElseThrow(() -> new CommentException(
            CommentErrorCode.COMMENT_NOT_FOUND,
            Map.of("commentId", commentId)
        ));

    // 좋아요를 취소한 사용자의 활동 문서에서 댓글 좋아요 활동을 제거하기 위한 Outbox 이벤트를 저장한다.
    userActivityOutboxService.saveCommentUnliked(userId, commentId);

    // 좋아요 대상 댓글 작성자의 활동 문서에 저장된 댓글 좋아요 수를 갱신하기 위한 Outbox 이벤트를 저장한다.
    userActivityOutboxService.saveCommentLikeCountUpdated(
        refreshed.getUser().getId(),
        refreshed.getId(),
        refreshed.getLikeCount()
    );

    // 좋아요를 취소한 사용자의 활동 문서에서 댓글 좋아요 활동을 제거한다.
    eventPublisher.publishEvent(
        new CommentUnlikedEvent(userId, commentId)
    );

    // 좋아요 대상 댓글 작성자의 활동 문서에 저장된 댓글 좋아요 수를 갱신한다.
    eventPublisher.publishEvent(
        new CommentLikeCountUpdatedEvent(
            refreshed.getUser().getId(),
            refreshed.getId(),
            refreshed.getLikeCount()
        )
    );

    log.info("좋아요 취소 완료 - commentId: {}, userId: {}", commentId, userId);
  }

  // 활성 댓글만 조회 (논리삭제된 것 제외)
  private Comment getActiveComment(UUID commentId) {
    return commentRepository
        .findByIdAndIsDeletedFalse(commentId)
        .orElseThrow(
            () ->
                new CommentException(
                    CommentErrorCode.COMMENT_NOT_FOUND, Map.of("commentId", commentId)));
  }

  // 활성 유저 조회 (존재 + 논리삭제 체크)
  private User getActiveUser(UUID userId) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(
                () -> new UserException(UserErrorCode.USER_NOT_FOUND, Map.of("userId", userId)));
    if (user.isDeleted()) {
      throw new UserException(UserErrorCode.USER_NOT_FOUND, Map.of("userId", userId));
    }
    return user;
  }

}
