package com.springboot.monew.users.event.comment;

import java.util.UUID;

public record CommentDeletedEvent(
    UUID userId,
    UUID commentId
) {

}
