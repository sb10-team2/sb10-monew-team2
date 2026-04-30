package com.springboot.monew.user.outbox.payload.commentlike;

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

}
