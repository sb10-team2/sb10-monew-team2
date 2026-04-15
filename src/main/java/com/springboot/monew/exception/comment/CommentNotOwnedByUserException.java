package com.springboot.monew.exception.comment;

import com.springboot.monew.exception.ErrorCode;

import java.util.Map;
import java.util.UUID;

public class CommentNotOwnedByUserException extends CommentException {
    public CommentNotOwnedByUserException(UUID commentId, UUID userId) {
        super(ErrorCode.COMMENT_NOT_OWNED_BY_USER, Map.of("commentId", commentId, "userId", userId));
    }
}
