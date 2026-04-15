package com.springboot.monew.exception.comment;

import com.springboot.monew.exception.ErrorCode;

import java.util.Map;
import java.util.UUID;

public class CommentLikeAlreadyExistsException extends CommentException {
    public CommentLikeAlreadyExistsException(UUID commentId, UUID userId) {
        super(ErrorCode.COMMENT_LIKE_ALREADY_EXISTS, Map.of("commentId", commentId, "userId", userId));
    }
}
