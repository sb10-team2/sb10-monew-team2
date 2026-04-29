package com.springboot.monew.user.event.comment;

import java.util.UUID;

public record CommentLikeCountUpdatedEvent(
    UUID userId,
    UUID commentId,
    Long likeCount
) {

}
