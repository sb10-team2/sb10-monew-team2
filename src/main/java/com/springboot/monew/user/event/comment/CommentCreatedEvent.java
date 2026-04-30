package com.springboot.monew.user.event.comment;

import com.springboot.monew.user.document.UserActivityDocument.CommentItem;
import java.util.UUID;

public record CommentCreatedEvent(
    UUID userId,
    CommentItem item
) {

}
