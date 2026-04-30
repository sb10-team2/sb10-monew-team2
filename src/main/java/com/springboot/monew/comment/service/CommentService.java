package com.springboot.monew.comment.service;

import com.springboot.monew.comment.dto.*;
import com.springboot.monew.comment.entity.Comment;
import com.springboot.monew.comment.exception.CommentErrorCode;
import com.springboot.monew.comment.exception.CommentException;
import com.springboot.monew.comment.mapper.CommentMapper;
import com.springboot.monew.comment.repository.CommentLikeRepository;
import com.springboot.monew.comment.repository.CommentRepository;
import com.springboot.monew.newsarticles.entity.NewsArticle;
import com.springboot.monew.newsarticles.exception.ArticleException;
import com.springboot.monew.newsarticles.exception.NewsArticleErrorCode;
import com.springboot.monew.newsarticles.repository.NewsArticleRepository;
import com.springboot.monew.user.document.UserActivityDocument.CommentItem;
import com.springboot.monew.user.entity.User;
import com.springboot.monew.user.exception.UserErrorCode;
import com.springboot.monew.user.exception.UserException;
import com.springboot.monew.user.repository.UserRepository;
import com.springboot.monew.user.service.UserActivityOutboxService;
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
  private final NewsArticleRepository articleRepository;
  private final UserRepository userRepository;
  private final CommentMapper commentMapper;
  private final UserActivityOutboxService userActivityOutboxService;

  // 댓글 등록
  @Transactional
  public CommentDto create(CommentRegisterRequest request) {
    // 존재하는 기사인지 check
    NewsArticle article = articleRepository.findById(request.articleId())
        .orElseThrow(() -> new ArticleException(NewsArticleErrorCode.NEWS_ARTICLE_NOT_FOUND, Map.of("articleId", request.articleId())));

    if (article.isDeleted()) {
      throw new ArticleException(NewsArticleErrorCode.NEWS_ARTICLE_ALREADY_DELETED, Map.of("articleId", request.articleId()));
    }

    User user = getActiveUser(request.userId());

    Comment comment = new Comment(user, article, request.content());
    commentRepository.save(comment);
    CommentItem item = commentMapper.toCommentItem(comment);
    // 댓글 생성 후 사용자 활동 반영을 위한 Outbox 이벤트를 저장한다.
    userActivityOutboxService.saveCommentCreated(user.getId(), item);
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
    CommentItem item = commentMapper.toCommentItem(comment);
    // 댓글 수정 후 사용자 활동 반영을 위한 Outbox 이벤트를 저장한다.
    userActivityOutboxService.saveCommentUpdated(userId, item);

    log.info("댓글 수정 완료 - commentId: {} userId: {}", commentId, userId);
    log.debug(
        "댓글 수정 완료 - commentId: {}, userId: {}, contentLength: {}",
        commentId,
        userId,
        comment.getContent().length());
    return commentMapper.toCommentDto(comment, likeByMe);
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
    // 삭제 되기 전에 사용자 활동내역에서 지울 userId만 먼저 추출(
    UUID userId = comment.getUser().getId();
    commentRepository.delete(comment);
    // 댓글 삭제 후 사용자 활동 반영을 위한 Outbox 이벤트를 저장한다.
    userActivityOutboxService.saveCommentDeleted(userId, commentId);
    log.info("댓글 물리 삭제 완료 - commentId: {}", commentId);
  }

  @Transactional(readOnly = true)
  public CursorPageResponseCommentDto<CommentDto> list(CommentPageRequest request, UUID userId) {
    // 기사 조회 -> 존재, 삭제 여부
    NewsArticle article = articleRepository.findById(request.articleId())
        .orElseThrow(() -> new ArticleException(NewsArticleErrorCode.NEWS_ARTICLE_NOT_FOUND, Map.of("articleId", request.articleId())));

    if (article.isDeleted()) {
      throw new ArticleException(NewsArticleErrorCode.NEWS_ARTICLE_ALREADY_DELETED, Map.of("articleId", request.articleId()));
    }

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
    long totalElements = commentRepository.countByArticleIdAndIsDeletedFalse(request.articleId());

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
