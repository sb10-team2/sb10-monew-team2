package com.springboot.monew.users.event.comment;

import com.springboot.monew.users.document.UserActivityDocument.CommentItem;
import java.util.UUID;

public record CommentUpdatedEvent(
    UUID userId,
    CommentItem item
) {

}
