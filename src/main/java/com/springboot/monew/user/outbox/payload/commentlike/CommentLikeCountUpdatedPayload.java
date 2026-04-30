package com.springboot.monew.user.outbox.payload.commentlike;

import java.util.UUID;

public record CommentLikeCountUpdatedPayload(
    UUID userId,
    UUID commentId,
    Long likeCount
) {

}
