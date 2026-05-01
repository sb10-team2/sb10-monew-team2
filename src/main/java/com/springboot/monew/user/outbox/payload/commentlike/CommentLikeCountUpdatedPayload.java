package com.springboot.monew.user.outbox.payload.commentlike;

import java.util.UUID;

public record CommentLikeCountUpdatedPayload(
    UUID userId,
    UUID commentId,
    Long likeCount
) {
  public static CommentLikeCountUpdatedPayload of(UUID userId, UUID commentId, long likeCount) {
    return new CommentLikeCountUpdatedPayload(userId, commentId, likeCount);
  }
}
