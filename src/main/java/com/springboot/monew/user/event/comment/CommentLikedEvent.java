package com.springboot.monew.user.event.comment;

import com.springboot.monew.user.document.UserActivityDocument.CommentLikeItem;
import java.util.UUID;

public record CommentLikedEvent(
    UUID userId,
    CommentLikeItem item
) {

}
