package com.springboot.monew.user.outbox.payload.comment;

import com.springboot.monew.user.event.comment.CommentDeletedEvent;
import java.util.UUID;

public record CommentDeletedPayload(
    UUID userId,
    UUID commentId
) {
  public static CommentDeletedPayload of(UUID userId, UUID commentId) {
    return new CommentDeletedPayload(userId, commentId);
  }

  public static CommentDeletedPayload of(CommentDeletedEvent event) {
    return new CommentDeletedPayload(
        event.userId(),
        event.commentId()
    );
  }
}
