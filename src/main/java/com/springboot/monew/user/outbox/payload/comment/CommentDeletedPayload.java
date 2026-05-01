package com.springboot.monew.user.outbox.payload.comment;

import java.util.UUID;

public record CommentDeletedPayload(
    UUID userId,
    UUID commentId
) {
  public static CommentDeletedPayload of(UUID userId, UUID commentId) {
    return new CommentDeletedPayload(userId, commentId);
  }
}
