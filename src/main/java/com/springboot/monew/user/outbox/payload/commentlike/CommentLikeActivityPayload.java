package com.springboot.monew.user.outbox.payload.commentlike;

import com.springboot.monew.user.document.UserActivityDocument.CommentLikeItem;
import java.time.Instant;
import java.util.UUID;

public record CommentLikeActivityPayload(
    UUID userId,
    UUID commentLikeId,
    Instant createdAt,
    UUID commentId,
    UUID articleId,
    String articleTitle,
    UUID commentUserId,
    String commentUserNickname,
    String commentContent,
    Long commentLikeCount,
    Instant commentCreatedAt
) {
  public static CommentLikeActivityPayload of(UUID userId, CommentLikeItem item) {
    return new CommentLikeActivityPayload(
        userId,
        item.id(),
        item.createdAt(),
        item.commentId(),
        item.articleId(),
        item.articleTitle(),
        item.commentUserId(),
        item.commentUserNickname(),
        item.commentContent(),
        item.commentLikeCount(),
        item.commentCreatedAt()
    );
  }
}
