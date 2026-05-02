package com.springboot.monew.user.outbox.payload.commentlike;

import com.springboot.monew.user.event.comment.CommentLikeCountUpdatedEvent;
import java.util.UUID;

public record CommentLikeCountUpdatedPayload(
    UUID userId,
    UUID commentId,
    Long likeCount
) {
  public static CommentLikeCountUpdatedPayload of(UUID userId, UUID commentId, long likeCount) {
    return new CommentLikeCountUpdatedPayload(userId, commentId, likeCount);
  }

  public static CommentLikeCountUpdatedPayload of(CommentLikeCountUpdatedEvent event) {
    return new CommentLikeCountUpdatedPayload(event.userId(), event.commentId(), event.likeCount());
  }
}
