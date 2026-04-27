package com.springboot.monew.users.event.comment;

import java.util.UUID;

public record CommentUnlikedEvent(
    UUID userId,
    UUID commentId
) {

}
