package com.springboot.monew.user.outbox.payload.comment;

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

}
