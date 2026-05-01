package com.springboot.monew.user.outbox.payload.commentlike;

import com.springboot.monew.user.event.comment.CommentUnlikedEvent;
import java.util.UUID;

public record CommentUnlikedPayload(
    UUID userId,
    UUID commentId
) {
  public static CommentUnlikedPayload of(UUID userId, UUID commentId) {
    return new CommentUnlikedPayload(userId, commentId);
  }

  public static CommentUnlikedPayload of(CommentUnlikedEvent event) {
    return new CommentUnlikedPayload(event.userId(), event.commentId());
  }
}
