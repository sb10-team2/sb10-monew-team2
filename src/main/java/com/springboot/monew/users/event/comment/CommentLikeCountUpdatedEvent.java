package com.springboot.monew.users.event.comment;

import java.util.UUID;

public record CommentLikeCountUpdatedEvent(
    UUID userId,
    UUID commentId,
    long likeCount
) {

}
