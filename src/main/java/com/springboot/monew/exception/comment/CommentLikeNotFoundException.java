package com.springboot.monew.exception.comment;

import com.springboot.monew.exception.ErrorCode;

import java.util.Map;
import java.util.UUID;

public class CommentLikeNotFoundException extends CommentException {
    public CommentLikeNotFoundException(UUID commentId, UUID userId) {
        super(ErrorCode.COMMENT_LIKE_NOT_FOUND, Map.of("commentId", commentId, "userId", userId));
    }
}
