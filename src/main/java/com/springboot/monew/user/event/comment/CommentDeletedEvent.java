package com.springboot.monew.user.event.comment;

import java.util.UUID;

public record CommentDeletedEvent(
    UUID userId,
    UUID commentId
) {

}
