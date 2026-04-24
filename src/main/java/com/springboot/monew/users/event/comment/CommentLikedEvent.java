package com.springboot.monew.users.event.comment;

import com.springboot.monew.users.document.UserActivityDocument.CommentLikeItem;
import java.util.UUID;

public record CommentLikedEvent(
    UUID userId,
    CommentLikeItem item
) {

}
