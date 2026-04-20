package com.springboot.monew.comment.service;

import com.springboot.monew.comment.dto.*;
import com.springboot.monew.comment.entity.Comment;
import com.springboot.monew.comment.entity.CommentLike;
import com.springboot.monew.comment.exception.CommentErrorCode;
import com.springboot.monew.comment.exception.CommentException;
import com.springboot.monew.comment.mapper.CommentLikeMapper;
import com.springboot.monew.comment.mapper.CommentMapper;
import com.springboot.monew.comment.repository.CommentLikeRepository;
import com.springboot.monew.comment.repository.CommentRepository;
import com.springboot.monew.users.entity.User;
import com.springboot.monew.users.exception.UserErrorCode;
import com.springboot.monew.users.exception.UserException;
import com.springboot.monew.users.repository.UserRepository;
import java.time.Instant;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentService {
  private final CommentRepository commentRepository;
  private final CommentLikeRepository commentLikeRepository;
  // TODO : Article 구현 시 주석 해제
  // private final ArticleRepository articleRepository;
  private final UserRepository userRepository;
  private final CommentMapper commentMapper;
  private final CommentLikeMapper commentLikeMapper;

  // 댓글 등록
  @Transactional
  public CommentDto create(CommentRegisterRequest request) {
    // TODO: Article 구현 시 주석 해제
    // 존재하는 기사인지 check
    // Article article = articleRepository.findById(request.articleId())
    // .orElseThrow(커스텀 예외);

    // TODO: Article 논리 삭제 check

    User user = getActiveUser(request.userId());

    // TODO: Article 추가
    Comment comment = new Comment(user, request.content());
    commentRepository.save(comment);
    log.info(
        "댓글 등록 완료 - commentId: {}, articleId: {}, userId: {}",
        comment.getId(),
        request.articleId(),
        request.userId());
    return commentMapper.toCommentDto(comment, false);
  }

  // 댓글 수정
  @Transactional
  public CommentDto update(UUID commentId, UUID userId, CommentUpdateRequest request) {
    Comment comment = getActiveComment(commentId);
    getActiveUser(userId);

    if (!comment.getUser().getId().equals(userId)) {
      throw new CommentException(
          CommentErrorCode.COMMENT_NOT_OWNED_BY_USER, Map.of("commentId", commentId));
    }

    comment.updateContent(request.content());
    boolean likeByMe = commentLikeRepository.existsByCommentIdAndUserId(commentId, userId);

    log.info("댓글 수정 완료 - commentId: {} userId: {}", commentId, userId);
    log.debug(
        "댓글 수정 완료 - commentId: {}, userId: {}, contentLength: {}",
        commentId,
        userId,
        comment.getContent().length());
    return commentMapper.toCommentDto(comment, likeByMe);
  }

  // 댓글 좋아요
  @Transactional
  public CommentLikeDto like(UUID commentId, UUID userId) {
    Comment comment = getActiveComment(commentId);
    User user = getActiveUser(userId);

    // 중복 좋아요 check
    if (commentLikeRepository.existsByCommentIdAndUserId(commentId, userId)) {
      throw new CommentException(
          CommentErrorCode.COMMENT_LIKE_ALREADY_EXISTS,
          Map.of("commentId", commentId, "userId", userId));
    }

    // Todo: 알림 객체 생성

    CommentLike commentLike = new CommentLike(comment, user);
    commentLikeRepository.save(commentLike);
    log.debug("likeCount 증가 전 - commentId: {}, likeCount: {}", commentId, comment.getLikeCount());
    commentRepository.incrementLikeCount(comment.getId());
    log.debug("likeCount 증가 후 - commentId: {}, likeCount: {}", commentId, comment.getLikeCount());
    log.info("좋아요 등록 완료 - commentId: {}, userId: {}", commentId, userId);

    return commentLikeMapper.toCommentLikeDto(commentLike);
  }

  // 댓글 논리 삭제
  @Transactional
  public void softDelete(UUID commentId) {
    Comment comment = getComment(commentId);

    if (comment.isDeleted()) {
      throw new CommentException(
          CommentErrorCode.COMMENT_ALREADY_DELETED, Map.of("commentId", commentId));
    }
    comment.delete();
    log.info("댓글 논리 삭제 완료 - commentId: {}", commentId);
  }

  // 댓글 물리 삭제
  @Transactional
  public void hardDelete(UUID commentId) {
    Comment comment = getComment(commentId);
    commentRepository.delete(comment);
    log.info("댓글 물리 삭제 완료 - commentId: {}", commentId);
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
    log.info("좋아요 취소 완료 - commentId: {}, userId: {}", commentId, userId);
  }

  // Todo: 기사의 전체 댓글 조회 -> Article 구현 끝나면 다시 체크 (고려해야할 것이 너무 많음)
  @Transactional(readOnly = true)
  public CursorPageResponseCommentDto<CommentDto> list(CommentPageRequest request, UUID userId) {
    // Todo: articleId 조회 -> 존재 + 소프드딜릿 여부

    // userId 조회 -> 존재 + 소프트딜릿 여부, 검증 느낌 ?
    User user = getActiveUser(userId);

    // Comment 조회
    List<Comment> comments =
        commentRepository.findComments(
            request.articleId(),
            request.orderBy(),
            request.direction(),
            request.cursor(),
            request.after(), // 보조 커서
            request.limit() + 1);

    // commentIds로 변경
    List<UUID> commentIds = comments.stream().map(Comment::getId).toList();

    // User가 좋아요 누른 댓글 Id만 가져옴
    Set<UUID> likeCommentIds =
        new HashSet<>(
            commentLikeRepository.findCommentIdsByUserIdAndCommentIdIn(userId, commentIds));

    // hasNext 판단
    boolean hasNext = comments.size() > request.limit();

    // hasNext true면 마지막 항목 자름
    if (hasNext) {
      comments = comments.subList(0, request.limit());
    }

    // 조건에 따라 주커서 가져옴
    String nextCursor =
        hasNext ? request.orderBy().getCursor(comments.get(comments.size() - 1)) : null;

    // 보조 커서 가져옴(createdAt)
    Instant nextAfter = hasNext ? comments.get(comments.size() - 1).getCreatedAt() : null;

    // size
    int size = comments.size();

    // 댓글 전체 개수
    int totalElements = 0; // Todo: countByArticleIdAndIsDeletedFalse(request.articleId());

    // Dto 변환
    List<CommentDto> content =
        comments.stream()
            .map(
                comment ->
                    commentMapper.toCommentDto(comment, likeCommentIds.contains(comment.getId())))
            .toList();

    return new CursorPageResponseCommentDto<CommentDto>(
        content, nextCursor, nextAfter, size, totalElements, hasNext);
  }

  // 논리삭제 여부 상관없이 댓글 조회
  private Comment getComment(UUID commentId) {
    return commentRepository
        .findById(commentId)
        .orElseThrow(
            () ->
                new CommentException(
                    CommentErrorCode.COMMENT_NOT_FOUND, Map.of("commentId", commentId)));
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
