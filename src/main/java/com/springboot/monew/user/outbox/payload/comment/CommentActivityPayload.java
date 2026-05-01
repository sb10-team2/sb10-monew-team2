package com.springboot.monew.user.outbox.payload.comment;

import com.springboot.monew.user.document.UserActivityDocument.CommentItem;
import java.time.Instant;
import java.util.UUID;

public record CommentActivityPayload(
    UUID userId,
    UUID commentId,
    UUID articleId,
    String articleTitle,
    UUID commentUserId,
    String commentUserNickname,
    String content,
    Long likeCount,
    Instant createdAt
) {
  public static CommentActivityPayload of(UUID userId, CommentItem item) {
    return new CommentActivityPayload(
        userId,
        item.id(),
        item.articleId(),
        item.articleTitle(),
        item.userId(),
        item.userNickname(),
        item.content(),
        item.likeCount(),
        item.createdAt()
    );
  }
}
