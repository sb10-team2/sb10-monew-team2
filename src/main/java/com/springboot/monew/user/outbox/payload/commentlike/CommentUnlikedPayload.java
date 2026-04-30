package com.springboot.monew.user.outbox.payload.commentlike;

import java.util.UUID;

public record CommentUnlikedPayload(
    UUID userId,
    UUID commentId
) {

}
