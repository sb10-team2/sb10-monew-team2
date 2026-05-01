package com.springboot.monew.user.outbox.payload.commentlike;

import java.util.UUID;

public record CommentUnlikedPayload(
    UUID userId,
    UUID commentId
) {
  public static CommentUnlikedPayload of(UUID userId, UUID commentId) {
    return new CommentUnlikedPayload(userId, commentId);
  }
}
